package com.slms.server;

import com.slms.dto.ApiResponse;
import com.slms.repository.ReaderRepository;
import com.slms.service.BookService;
import com.slms.service.LoanService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
public class StatisticsHandler implements HttpHandler {

    private static final int WIDTH = 900;
    private static final int HEIGHT = 360;
    private static final String[] LABELS = {"Đầu sách", "Độc giả", "Mượn trong hạn", "Quá hạn"};
    private static final Color[] BAR_COLORS = {
            new Color(37, 99, 235),
            new Color(22, 163, 74),
            new Color(217, 119, 6),
            new Color(220, 38, 38)
    };

    private final BookService bookService = new BookService();
    private final ReaderRepository readerRepository = new ReaderRepository();
    private final LoanService loanService = new LoanService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpHelper.enableCors(exchange);
        String method = exchange.getRequestMethod().toUpperCase();

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if (!"GET".equals(method)) {
            HttpHelper.sendJsonResponse(exchange, 405, ApiResponse.error("Phương thức không được hỗ trợ"));
            return;
        }

        byte[] chart;
        try {
            chart = renderChart();
        } catch (RuntimeException | IOException e) {
            System.err.println(">>> Lỗi khi tạo biểu đồ thống kê: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, ApiResponse.error("Không thể tạo biểu đồ thống kê"));
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "image/png");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, chart.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(chart);
        }
    }

    private byte[] renderChart() throws IOException {
        long[] values = {
                bookService.getAllBooks().size(),
                readerRepository.findAll().size(),
                loanService.getBorrowedLoans().size(),
                loanService.getOverdueLoans().size()
        };
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, WIDTH, HEIGHT);

            int left = 92;
            int right = WIDTH - 34;
            int top = 36;
            int bottom = HEIGHT - 72;
            int plotHeight = bottom - top;
            long maximum = 0;
            for (long value : values) {
                maximum = Math.max(maximum, value);
            }
            long tickStep = Math.max(1, (maximum + 3) / 4);
            long axisMaximum = tickStep * 4;

            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            FontMetrics metrics = graphics.getFontMetrics();
            graphics.setStroke(new BasicStroke(1));
            for (int tick = 0; tick <= 4; tick++) {
                long tickValue = tickStep * tick;
                int y = bottom - (int) (tickValue * plotHeight / axisMaximum);
                graphics.setColor(new Color(226, 232, 240));
                graphics.drawLine(left, y, right, y);
                graphics.setColor(new Color(100, 116, 139));
                String label = Long.toString(tickValue);
                graphics.drawString(label, left - metrics.stringWidth(label) - 12, y + 5);
            }

            int plotWidth = right - left;
            int slotWidth = plotWidth / LABELS.length;
            int barWidth = 94;
            for (int i = 0; i < values.length; i++) {
                int barHeight = (int) (values[i] * plotHeight / axisMaximum);
                int x = left + i * slotWidth + (slotWidth - barWidth) / 2;
                int y = bottom - barHeight;

                graphics.setColor(BAR_COLORS[i]);
                if (barHeight > 0) {
                    graphics.fillRoundRect(x, y, barWidth, barHeight, 10, 10);
                }

                graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                graphics.setColor(new Color(15, 23, 42));
                String valueLabel = Long.toString(values[i]);
                FontMetrics valueMetrics = graphics.getFontMetrics();
                graphics.drawString(valueLabel, x + (barWidth - valueMetrics.stringWidth(valueLabel)) / 2,
                        Math.max(top + 16, y - 9));

                graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                graphics.setColor(new Color(71, 85, 105));
                FontMetrics labelMetrics = graphics.getFontMetrics();
                String category = LABELS[i];
                graphics.drawString(category,
                        left + i * slotWidth + (slotWidth - labelMetrics.stringWidth(category)) / 2,
                        bottom + 30);
            }

            graphics.setColor(new Color(148, 163, 184));
            graphics.drawLine(left, top, left, bottom);
            graphics.drawLine(left, bottom, right, bottom);

            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (!ImageIO.write(image, "png", output)) {
                    throw new IOException("Không tìm thấy bộ mã hóa PNG");
                }
                return output.toByteArray();
            }
        } finally {
            graphics.dispose();
        }
    }
}
