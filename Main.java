import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Main {
    static double numeric(String value) {
        double v = Double.parseDouble(value);
        if (!Double.isFinite(v)) throw new IllegalArgumentException("finite number required");
        return v;
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
    static List<String[]> input(int columns) throws IOException {
        var rows = new ArrayList<String[]>();
        var reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            require(rows.size() < 100000, "input limit exceeded");
            require(line.length() <= 8192, "input record too large");
            var row = line.split(",", -1);
            require(row.length == columns && Arrays.stream(row).noneMatch(String::isBlank), "invalid CSV row");
            rows.add(row);
        }
        require(!rows.isEmpty(), "missing input");
        return rows;
    }
    public static void main(String[] args) {
        try {
            require(args.length == 0 || (args.length == 1 && args[0].equals("--self-test")), "unsupported command arguments");
            if (args.length == 1 && args[0].equals("--self-test")) { test(); System.out.println("tests passed"); }
            else run();
        } catch (Exception e) { System.err.println(e.getMessage()); System.exit(2); }
    }

    record Event(String event, String operation, String part, String reservation, int quantity) {}
    static final class Ledger {
        final Map<String, Integer> stock = new TreeMap<>();
        final Map<String, Event> reservations = new HashMap<>();
        final Map<String, Event> events = new HashMap<>();
        void apply(Event e) {
            require(e.quantity > 0, "quantity must be positive");
            if (events.containsKey(e.event)) { require(events.get(e.event).equals(e), "event id collision"); return; }
            int quantity = stock.getOrDefault(e.part, 0);
            switch (e.operation) {
                case "RECEIVE" -> stock.put(e.part, Math.addExact(quantity, e.quantity));
                case "RESERVE" -> {
                    require(!reservations.containsKey(e.reservation), "reservation already exists");
                    int held = reservations.values().stream().filter(r -> r.part.equals(e.part)).mapToInt(Event::quantity).sum();
                    require(quantity - held >= e.quantity, "insufficient available stock"); reservations.put(e.reservation, e);
                }
                case "RELEASE", "ISSUE" -> {
                    var held = reservations.get(e.reservation);
                    require(held != null && held.part.equals(e.part) && held.quantity == e.quantity, "reservation mismatch");
                    if (e.operation.equals("ISSUE")) stock.put(e.part, quantity - e.quantity);
                    reservations.remove(e.reservation);
                }
                default -> throw new IllegalArgumentException("unknown operation");
            }
            events.put(e.event, e);
        }
    }
    static void test() {
        var l = new Ledger(); var receive = new Event("1", "RECEIVE", "seal", "-", 5); l.apply(receive); l.apply(receive);
        l.apply(new Event("2", "RESERVE", "seal", "job", 4));
        boolean rejected = false; try { l.apply(new Event("3", "RESERVE", "seal", "other", 2)); } catch (IllegalArgumentException e) { rejected = true; }
        require(rejected && l.stock.get("seal") == 5 && l.events.size() == 2, "transaction rollback mismatch");
        l.apply(new Event("4", "ISSUE", "seal", "job", 4)); require(l.stock.get("seal") == 1, "issue mismatch");
    }
    static void run() throws IOException {
        var l = new Ledger();
        for (var r : input(5)) l.apply(new Event(r[0], r[1], r[2], r[3], Integer.parseInt(r[4])));
        l.stock.forEach((part, quantity) -> { int held = l.reservations.values().stream().filter(r -> r.part.equals(part)).mapToInt(Event::quantity).sum(); System.out.printf("%s,on_hand=%d,reserved=%d,available=%d%n", part, quantity, held, quantity - held); });
    }

}
