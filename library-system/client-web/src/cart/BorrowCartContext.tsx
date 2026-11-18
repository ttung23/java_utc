import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';

export type CartItem = {
  bookId: number;
  title: string;
  coverUrl?: string;
  authorNames?: string[];
  publisherName?: string;
  isbn13?: string;
  availableCopies?: number;
  totalCopies?: number;
  classificationCode?: string;
};

type BorrowCartContextValue = {
  items: CartItem[];
  count: number;
  addItem: (item: CartItem) => boolean;
  removeItem: (bookId: number) => void;
  clear: () => void;
  hasItem: (bookId: number) => boolean;
};

const STORAGE_KEY = 'library_client_borrow_cart';
const MAX_ITEMS = 5;

const BorrowCartContext = createContext<BorrowCartContextValue | null>(null);

function readStorage(): CartItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as CartItem[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function BorrowCartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>(() => readStorage());

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  }, [items]);

  const addItem = useCallback((item: CartItem) => {
    let ok = true;
    setItems((prev) => {
      if (prev.some((x) => x.bookId === item.bookId)) {
        ok = false;
        return prev;
      }
      if (prev.length >= MAX_ITEMS) {
        ok = false;
        return prev;
      }
      return [...prev, item];
    });
    return ok;
  }, []);

  const removeItem = useCallback((bookId: number) => {
    setItems((prev) => prev.filter((x) => x.bookId !== bookId));
  }, []);

  const clear = useCallback(() => setItems([]), []);

  const hasItem = useCallback((bookId: number) => items.some((x) => x.bookId === bookId), [items]);

  const value = useMemo(
    () => ({ items, count: items.length, addItem, removeItem, clear, hasItem }),
    [items, addItem, removeItem, clear, hasItem],
  );

  return <BorrowCartContext.Provider value={value}>{children}</BorrowCartContext.Provider>;
}

export function useBorrowCart() {
  const ctx = useContext(BorrowCartContext);
  if (!ctx) throw new Error('useBorrowCart must be used within BorrowCartProvider');
  return ctx;
}

export const CART_MAX_ITEMS = MAX_ITEMS;
