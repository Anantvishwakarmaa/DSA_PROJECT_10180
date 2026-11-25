
import java.io.IOException;
import static java.lang.System.out;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class dsa {
    private static final int FIRST_ID = 101;
    private static final int LOW_STOCK = 5;
    private static final Path STORE = Path.of("inventory.txt");

    private static final String MENU = String.join(System.lineSeparator(), "",
            "1. Add item", "2. Sell item", "3. Display sorted by name",
            "4. Low-stock report", "5. Save and exit", "Choose an option: ");

    private static final class Item {
        final int number;
        final String name;
        int quantity;
        final BigDecimal price;

        Item(int number, String name, int quantity, BigDecimal price) {
            this.number = number;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        String id() {
            return "G" + number;
        }

        String details() {
            return id() + " | " + name + " | Qty: " + quantity
                    + " | Price: " + price.stripTrailingZeros().toPlainString();
        }
    }

    private final ArrayList<Item> inventory = new ArrayList<>();
    private final HashMap<String, Item> byId = new HashMap<>();
    private int nextId = FIRST_ID;

    private static String readLine(Scanner input, String prompt) {
        out.print(prompt);
        return input.hasNextLine() ? input.nextLine().strip() : null;
    }

    private static boolean validName(String name) {
        return !name.isBlank() && !name.contains("\t");
    }

    private void addRecord(Item item) {
        inventory.add(item);
        byId.put(item.id(), item);
    }

    private void addItem(Scanner input) {
        String name = readLine(input, "Item: ");
        String quantityText = name == null ? null : readLine(input, "Quantity: ");
        String priceText = quantityText == null ? null : readLine(input, "Price: ");

        if (priceText == null) {
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityText);
            BigDecimal price = new BigDecimal(priceText);

            if (!validName(name) || quantity < 0 || price.signum() < 0) {
                out.println("Invalid item data. Item not added.");
            } else if (nextId == Integer.MAX_VALUE) {
                out.println("Item IDs exhausted. Item not added.");
            } else {
                Item item = new Item(nextId++, name, quantity, price);
                addRecord(item);
                out.println("Item added. ID: " + item.details());
            }
        } catch (NumberFormatException e) {
            out.println("Invalid item data. Item not added.");
        }
    }

    private void sellItem(Scanner input) {
        String id = readLine(input, "Item ID: ");
        String quantityText = id == null ? null : readLine(input, "Quantity: ");

        if (quantityText == null) {
            return;
        }

        int quantity;

        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            quantity = 0;
        }

        Item item = byId.get(id.toUpperCase(Locale.ROOT));

        if (item == null) {
            out.println("Item not found.");
        } else if (quantity <= 0) {
            out.println("Invalid quantity. Sale not done.");
        } else if (quantity > item.quantity) {
            out.println("Insufficient stock. Available: " + item.quantity);
        } else {
            item.quantity -= quantity;
            out.printf("Sold %d of %s. Stock: %d%s%n", quantity, item.name,
                    item.quantity, item.quantity == 0 ? " (Out of stock)" : "");
        }
    }

    private void displaySorted() {
        if (inventory.isEmpty()) {
            out.println("Inventory is empty.");
            return;
        }

        ArrayList<Item> sorted = new ArrayList<>(inventory);

        // Stable insertion sort
        for (int i = 1; i < sorted.size(); i++) {
            Item current = sorted.get(i);
            int j = i - 1;

            while (j >= 0
                    && sorted.get(j).name.compareToIgnoreCase(current.name) > 0) {
                sorted.set(j + 1, sorted.get(j));
                j--;
            }

            sorted.set(j + 1, current);
        }

        for (int i = 0; i < sorted.size(); i++) {
            out.println((i + 1) + ". " + sorted.get(i).details());
        }
    }

    private void lowStockReport() {
        boolean found = false;

        for (Item item : inventory) {
            if (item.quantity < LOW_STOCK) {
                out.printf("Low stock: %s | %s | Qty: %d%n",
                        item.id(), item.name, item.quantity);
                found = true;
            }
        }

        if (!found) {
            out.println("No low-stock items.");
        }
    }

    private void load() throws IOException {
        try {
            List<String> lines = Files.readAllLines(STORE);

            if (lines.isEmpty()) {
                throw new IOException("Missing next item ID.");
            }

            nextId = Integer.parseInt(lines.get(0));

            if (nextId < FIRST_ID) {
                throw new IOException("Invalid next item ID.");
            }

            int previousId = FIRST_ID - 1;

            for (int i = 1; i < lines.size(); i++) {
                String[] fields = lines.get(i).split("\t", -1);

                if (fields.length != 4) {
                    throw new IOException(
                            "Invalid item record on line " + (i + 1) + ".");
                }

                int number = Integer.parseInt(fields[0]);
                int quantity = Integer.parseInt(fields[1]);
                BigDecimal price = new BigDecimal(fields[2]);
                String name = fields[3];

                if (number <= previousId || number >= nextId || quantity < 0
                        || price.signum() < 0 || !validName(name)) {
                    throw new IOException(
                            "Invalid item data on line " + (i + 1) + ".");
                }

                addRecord(new Item(number, name, quantity, price));
                previousId = number;
            }
        } catch (NoSuchFileException e) {
            // First run starts with an empty inventory.
        } catch (NumberFormatException e) {
            throw new IOException("A saved ID, quantity or price is invalid.", e);
        }
    }

    private void save() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(Integer.toString(nextId));

        for (Item item : inventory) {
            lines.add(item.number + "\t" + item.quantity + "\t"
                    + item.price.toPlainString() + "\t" + item.name);
        }

        Path temporary = Files.createTempFile(Path.of("."), "inventory-", ".tmp");

        try {
            Files.write(temporary, lines);
            Files.move(temporary, STORE, REPLACE_EXISTING, ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, STORE, REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void run(Scanner input) {
        while (true) {
            String option = readLine(input, MENU);

            if (option == null || option.equals("5")) {
                return;
            }

            switch (option) {
                case "1":
                    addItem(input);
                    break;
                case "2":
                    sellItem(input);
                    break;
                case "3":
                    displaySorted();
                    break;
                case "4":
                    lowStockReport();
                    break;
                default:
                    out.println("Invalid option. Enter 1 to 5.");
            }
        }
    }

    public static void main(String[] args) {
        dsa app = new dsa();

        try (Scanner input = new Scanner(System.in, StandardCharsets.UTF_8)) {
            app.load();
            app.run(input);
            app.save();
            out.println("Inventory saved to inventory.txt.");
        } catch (IOException e) {
            System.err.println("Error with inventory.txt: " + e.getMessage());
            System.exit(1);
        }
    }
}

