import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


class InsufficientFundsException extends Exception {

    private final double attempted;
    private final double available;

    InsufficientFundsException(double attempted, double available) {
        super(String.format(
            "Insufficient funds: attempted to withdraw %.2f but only %.2f available.",
            attempted, available
        ));
        this.attempted = attempted;
        this.available = available;
    }

    double getAttempted() { return attempted; }
    double getAvailable() { return available; }
    double getShortfall() { return attempted - available; }
}


class Transaction {

    enum Type { DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN, OPENING }

    private final Type          type;
    private final double        amount;
    private final double        runningBalance;
    private final LocalDateTime timestamp;
    private final String        note;

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    Transaction(Type type, double amount, double runningBalance, String note) {
        this.type           = type;
        this.amount         = amount;
        this.runningBalance = runningBalance;
        this.timestamp      = LocalDateTime.now();
        this.note           = note;
    }

    Type          getType()           { return type;           }
    double        getAmount()         { return amount;         }
    double        getRunningBalance() { return runningBalance; }
    LocalDateTime getTimestamp()      { return timestamp;      }
    String        getNote()           { return note;           }

    @Override
    public String toString() {
        String sign = (type == Type.WITHDRAWAL || type == Type.TRANSFER_OUT) ? "-" : "+";
        if (type == Type.OPENING) sign = " ";
        return String.format(
            "  [%s]  %-14s  %s%9.2f   Balance: %10.2f   %s",
            timestamp.format(FMT),
            type,
            sign,
            amount,
            runningBalance,
            note.isEmpty() ? "" : "(" + note + ")"
        );
    }
}


public class BankAccount {

    private final String              id;
    private final String              owner;
    private       double              balance;
    private final List<Transaction>   history;
    private       boolean             frozen;

    private static final double MAX_SINGLE_WITHDRAWAL = 10_000.00;
    private static final double OVERDRAFT_LIMIT       =     0.00;

    BankAccount(String owner, double openingBalance) {
        if (openingBalance < 0)
            throw new IllegalArgumentException("Opening balance cannot be negative.");
        this.id      = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.owner   = owner;
        this.balance = openingBalance;
        this.history = new ArrayList<>();
        this.frozen  = false;

        history.add(new Transaction(
            Transaction.Type.OPENING,
            openingBalance,
            openingBalance,
            "Account opened"
        ));
    }

    String getId()      { return id;      }
    String getOwner()   { return owner;   }
    double getBalance() { return balance; }
    boolean isFrozen()  { return frozen;  }

    List<Transaction> getHistory() {
        return Collections.unmodifiableList(history);
    }

    void freeze()   { frozen = true;  }
    void unfreeze() { frozen = false; }

    void deposit(double amount) throws IllegalArgumentException {
        validateNotFrozen();
        if (amount <= 0)
            throw new IllegalArgumentException("Deposit amount must be positive.");
        balance += amount;
        history.add(new Transaction(Transaction.Type.DEPOSIT, amount, balance, ""));
    }

    void deposit(double amount, String note) throws IllegalArgumentException {
        validateNotFrozen();
        if (amount <= 0)
            throw new IllegalArgumentException("Deposit amount must be positive.");
        balance += amount;
        history.add(new Transaction(Transaction.Type.DEPOSIT, amount, balance, note));
    }

    void withdraw(double amount) throws InsufficientFundsException {
        validateNotFrozen();
        if (amount <= 0)
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        if (amount > MAX_SINGLE_WITHDRAWAL)
            throw new IllegalArgumentException(
                "Single withdrawal limit is " + MAX_SINGLE_WITHDRAWAL);
        if (balance - amount < OVERDRAFT_LIMIT)
            throw new InsufficientFundsException(amount, balance);
        balance -= amount;
        history.add(new Transaction(Transaction.Type.WITHDRAWAL, amount, balance, ""));
    }

    void withdraw(double amount, String note) throws InsufficientFundsException {
        validateNotFrozen();
        if (amount <= 0)
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        if (amount > MAX_SINGLE_WITHDRAWAL)
            throw new IllegalArgumentException(
                "Single withdrawal limit is " + MAX_SINGLE_WITHDRAWAL);
        if (balance - amount < OVERDRAFT_LIMIT)
            throw new InsufficientFundsException(amount, balance);
        balance -= amount;
        history.add(new Transaction(Transaction.Type.WITHDRAWAL, amount, balance, note));
    }

    void transferTo(BankAccount target, double amount) throws InsufficientFundsException {
        validateNotFrozen();
        target.validateNotFrozen();
        if (amount <= 0)
            throw new IllegalArgumentException("Transfer amount must be positive.");
        if (balance - amount < OVERDRAFT_LIMIT)
            throw new InsufficientFundsException(amount, balance);
        balance -= amount;
        history.add(new Transaction(
            Transaction.Type.TRANSFER_OUT, amount, balance,
            "to " + target.getOwner() + " [" + target.getId() + "]"
        ));
        target.balance += amount;
        target.history.add(new Transaction(
            Transaction.Type.TRANSFER_IN, amount, target.balance,
            "from " + owner + " [" + id + "]"
        ));
    }

    double totalDeposited() {
        return history.stream()
            .filter(t -> t.getType() == Transaction.Type.DEPOSIT
                      || t.getType() == Transaction.Type.TRANSFER_IN)
            .mapToDouble(Transaction::getAmount)
            .sum();
    }

    double totalWithdrawn() {
        return history.stream()
            .filter(t -> t.getType() == Transaction.Type.WITHDRAWAL
                      || t.getType() == Transaction.Type.TRANSFER_OUT)
            .mapToDouble(Transaction::getAmount)
            .sum();
    }

    List<Transaction> getHistoryByType(Transaction.Type type) {
        return history.stream()
            .filter(t -> t.getType() == type)
            .collect(java.util.stream.Collectors.toList());
    }

    void printStatement() {
        System.out.println();
        System.out.println("═".repeat(90));
        System.out.printf("  ACCOUNT STATEMENT — %s  [ID: %s]%n", owner.toUpperCase(), id);
        System.out.println("═".repeat(90));
        for (Transaction t : history) System.out.println(t);
        System.out.println("─".repeat(90));
        System.out.printf("  Total deposited : +%.2f%n", totalDeposited());
        System.out.printf("  Total withdrawn :  -%.2f%n", totalWithdrawn());
        System.out.printf("  Current balance :   %.2f%n", balance);
        System.out.println("═".repeat(90));
        System.out.println();
    }

    private void validateNotFrozen() {
        if (frozen)
            throw new IllegalStateException("Account [" + id + "] is frozen.");
    }

    @Override
    public String toString() {
        return String.format("BankAccount{id='%s', owner='%s', balance=%.2f, frozen=%b, txns=%d}",
            id, owner, balance, frozen, history.size());
    }
}