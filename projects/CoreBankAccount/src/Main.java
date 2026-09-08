public class Main {

    static final String RESET  = "\033[0m";
    static final String GREEN  = "\033[92m";
    static final String CYAN   = "\033[96m";
    static final String YELLOW = "\033[93m";
    static final String RED    = "\033[91m";
    static final String DIM    = "\033[2m";
    static final String BOLD   = "\033[1m";

    public static void main(String[] args) {
        section("1. Opening Accounts");
        demo_openingAccounts();

        section("2. Deposits");
        demo_deposits();

        section("3. Withdrawals + InsufficientFundsException");
        demo_withdrawals();

        section("4. Transfer Between Accounts");
        demo_transfers();

        section("5. Full Transaction History / Statement");
        demo_statement();

        section("6. Frozen Account Guard");
        demo_frozenAccount();

        section("7. Edge Cases");
        demo_edgeCases();
    }

    static void demo_openingAccounts() {
        BankAccount alice = new BankAccount("Alice", 1_000.00);
        BankAccount bob   = new BankAccount("Bob",       0.00);

        ok("Alice opened:  " + alice);
        ok("Bob opened:    " + bob);
    }

    static void demo_deposits() {
        BankAccount acc = new BankAccount("Carol", 500.00);
        info("Initial balance: " + fmt(acc.getBalance()));

        try {
            acc.deposit(250.00);
            ok("Deposited 250.00  → " + fmt(acc.getBalance()));

            acc.deposit(1_000.00, "paycheck");
            ok("Deposited 1000.00 (paycheck) → " + fmt(acc.getBalance()));

            acc.deposit(-10.00);
        } catch (IllegalArgumentException e) {
            warn("Rejected negative deposit: " + e.getMessage());
        }
    }

    static void demo_withdrawals() {
        BankAccount acc = new BankAccount("Dave", 300.00);
        info("Initial balance: " + fmt(acc.getBalance()));

        try {
            acc.withdraw(100.00, "grocery run");
            ok("Withdrew 100.00 → " + fmt(acc.getBalance()));
        } catch (InsufficientFundsException e) {
            err(e.getMessage());
        }

        try {
            acc.withdraw(500.00);
        } catch (InsufficientFundsException e) {
            warn("Caught InsufficientFundsException:");
            warn("  Attempted : " + fmt(e.getAttempted()));
            warn("  Available : " + fmt(e.getAvailable()));
            warn("  Shortfall : " + fmt(e.getShortfall()));
        }

        try {
            acc.withdraw(15_000.00);
        } catch (InsufficientFundsException e) {
            err(e.getMessage());
        } catch (IllegalArgumentException e) {
            warn("Rejected over-limit withdrawal: " + e.getMessage());
        }
    }

    static void demo_transfers() {
        BankAccount alice = new BankAccount("Alice", 2_000.00);
        BankAccount bob   = new BankAccount("Bob",     500.00);

        info("Before — Alice: " + fmt(alice.getBalance()) + "  Bob: " + fmt(bob.getBalance()));

        try {
            alice.transferTo(bob, 750.00);
            ok("Transferred 750.00 from Alice → Bob");
            ok("After  — Alice: " + fmt(alice.getBalance()) + "  Bob: " + fmt(bob.getBalance()));

            alice.transferTo(bob, 5_000.00);
        } catch (InsufficientFundsException e) {
            warn("Transfer rejected: " + e.getMessage());
        }
    }

    static void demo_statement() {
        BankAccount acc = new BankAccount("Eve", 1_000.00);
        BankAccount other = new BankAccount("Frank", 200.00);

        try {
            acc.deposit(500.00, "freelance invoice");
            acc.withdraw(120.00, "utilities");
            acc.deposit(300.00, "refund");
            acc.transferTo(other, 400.00);
            acc.withdraw(80.00, "coffee");
        } catch (InsufficientFundsException e) {
            err(e.getMessage());
        }

        acc.printStatement();

        info("Total deposited : " + fmt(acc.totalDeposited()));
        info("Total withdrawn : " + fmt(acc.totalWithdrawn()));
        info("Tx count (deposits only): " +
            acc.getHistoryByType(Transaction.Type.DEPOSIT).size());
    }

    static void demo_frozenAccount() {
        BankAccount acc = new BankAccount("Grace", 1_000.00);
        acc.freeze();
        info("Account frozen: " + acc.isFrozen());

        try {
            acc.deposit(100.00);
        } catch (IllegalStateException e) {
            warn("Deposit blocked: " + e.getMessage());
        }

        try {
            acc.withdraw(50.00);
        } catch (InsufficientFundsException e) {
            err(e.getMessage());
        } catch (IllegalStateException e) {
            warn("Withdrawal blocked: " + e.getMessage());
        }

        acc.unfreeze();
        ok("Account unfrozen.");
        try {
            acc.deposit(100.00);
            ok("Deposit after unfreeze: " + fmt(acc.getBalance()));
        } catch (Exception e) {
            err(e.getMessage());
        }
    }

    static void demo_edgeCases() {
        try {
            new BankAccount("Bad", -500.00);
        } catch (IllegalArgumentException e) {
            warn("Rejected negative opening balance: " + e.getMessage());
        }

        BankAccount acc = new BankAccount("Hank", 100.00);
        try {
            acc.deposit(0);
        } catch (IllegalArgumentException e) {
            warn("Rejected zero deposit: " + e.getMessage());
        }

        try {
            acc.withdraw(0);
        } catch (IllegalArgumentException e) {
            warn("Rejected zero withdrawal: " + e.getMessage());
        } catch (InsufficientFundsException e) {
            err(e.getMessage());
        }

        try {
            acc.withdraw(100.00);
            ok("Exact balance withdrawal OK → " + fmt(acc.getBalance()));
        } catch (InsufficientFundsException e) {
            err(e.getMessage());
        }

        try {
            acc.withdraw(0.01);
        } catch (InsufficientFundsException e) {
            warn("One cent over empty account: " + e.getMessage());
        }
    }

    static void section(String title) {
        System.out.println();
        System.out.println(BOLD + CYAN + "── " + title + " " + "─".repeat(Math.max(0, 60 - title.length())) + RESET);
    }

    static void ok(String msg)   { System.out.println(GREEN  + "  ✓ " + msg + RESET); }
    static void info(String msg) { System.out.println(DIM    + "  · " + msg + RESET); }
    static void warn(String msg) { System.out.println(YELLOW + "  ⚠ " + msg + RESET); }
    static void err(String msg)  { System.out.println(RED    + "  ✗ " + msg + RESET); }

    static String fmt(double amount) {
        return String.format("$%.2f", amount);
    }
}