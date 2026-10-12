package lodge.controller;

import lodge.model.Bill;
import lodge.model.BillCsvExporter;
import lodge.model.BillRepository;
import lodge.model.BillingCalculator;
import lodge.model.ReceiptFormatter;
import lodge.view.MainWindow;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static lodge.model.BillingCalculator.money;

/**
 * CONTROLLER: reacts to what the user does in the view, asks the model to do the work,
 * and tells the view what to show. It is the only class that knows about both sides.
 */
public final class BillController implements MainWindow.Listener {

    private static final int MAX_GUEST_LENGTH = 60;
    private static final int MAX_NOTES_LENGTH = 300;

    private final String username;
    private final BillRepository repository;
    private final MainWindow view;
    private final Runnable logoutAction;
    private final Runnable footerListener = this::refreshFooter;

    public BillController(String username, BillRepository repository, MainWindow view, Runnable logoutAction) {
        this.username = username;
        this.repository = repository;
        this.view = view;
        this.logoutAction = logoutAction;

        view.setListener(this);
        repository.addChangeListener(footerListener);
        refreshFooter();
        updateSummary();
    }

    // ---------- live bill ----------

    @Override
    public void onFormChanged() {
        updateSummary();
    }

    private void updateSummary() {
        Optional<Integer> nights = InputParser.nights(view.nightsText());
        Optional<BigDecimal> extras = InputParser.money(view.extrasText());
        Optional<BigDecimal> paid = InputParser.money(view.paidText());
        view.markInvalid(nights.isEmpty(), extras.isEmpty(), paid.isEmpty());
        if (nights.isEmpty() || extras.isEmpty() || paid.isEmpty()) {
            view.showInvalidSummary();
            return;
        }
        view.showSummary(BillingCalculator.calculate(view.roomType(), nights.get(), extras.get(), paid.get()));
    }

    // ---------- actions ----------

    @Override
    public void onSave() {
        String guest = view.guestName().trim();
        if (guest.isEmpty()) {
            view.showError("Please enter the guest name.");
            view.focusGuest();
            return;
        }
        if (guest.length() > MAX_GUEST_LENGTH) {
            view.showError("The guest name must be " + MAX_GUEST_LENGTH + " characters or fewer.");
            view.focusGuest();
            return;
        }
        Optional<Integer> nights = InputParser.nights(view.nightsText());
        if (nights.isEmpty() || nights.get() < 1) {
            view.showError("Nights must be a whole number from 1 to " + InputParser.MAX_NIGHTS + ".");
            view.focusNights();
            return;
        }
        Optional<BigDecimal> extras = InputParser.money(view.extrasText());
        Optional<BigDecimal> paid = InputParser.money(view.paidText());
        if (extras.isEmpty() || paid.isEmpty()) {
            view.showError("Extras and amount paid must be valid amounts (0 or more, at most 2 decimal places).");
            return;
        }
        String notes = view.notesText().trim();
        if (notes.length() > MAX_NOTES_LENGTH) {
            view.showError("Notes must be " + MAX_NOTES_LENGTH + " characters or fewer (now " + notes.length() + ").");
            return;
        }

        BillingCalculator.Result r = BillingCalculator.calculate(view.roomType(), nights.get(), extras.get(), paid.get());
        Bill bill = new Bill(repository.nextId(), guest, view.roomType(), nights.get(),
                r.extras(), r.paid(), notes,
                r.roomCharge(), r.discount(), r.total(), r.balance(),
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), username);
        try {
            repository.add(bill);
        } catch (IOException e) {
            view.showError("Could not save", "The bill could not be saved to disk:\n" + e.getMessage());
            return;
        }
        view.setStatus("Saved " + bill.id() + ": " + balanceText(bill));
        view.clearForm();
        updateSummary();
        view.revealRow(repository.size() - 1);
    }

    @Override
    public void onNewGuest() {
        if (!confirmDiscardIfDirty()) return;
        view.clearForm();
        updateSummary();
    }

    @Override
    public void onViewReceipt() {
        Bill b = selectedBill();
        if (b == null) return;
        view.showReceipt(b.id(), ReceiptFormatter.format(b));
    }

    @Override
    public void onDelete() {
        Bill b = selectedBill();
        if (b == null) return;
        if (!view.confirm("Confirm delete", "Delete bill " + b.id() + " for " + b.guest() + "?")) return;
        try {
            repository.remove(view.selectedRow());
        } catch (IOException e) {
            view.showError("Could not delete", "The change could not be saved to disk:\n" + e.getMessage());
            return;
        }
        view.setStatus("Deleted " + b.id());
    }

    @Override
    public void onExport() {
        if (repository.size() == 0) {
            view.showInfo("There are no bills to export yet.");
            return;
        }
        Path target = view.chooseSaveFile("lodge-bills-" + LocalDate.now() + ".csv", "CSV file (*.csv)", "csv");
        if (target == null) return;
        try {
            BillCsvExporter.write(repository.all(), target);
            view.setStatus("Exported " + repository.size() + " bill" + (repository.size() == 1 ? "" : "s")
                    + " to " + target.getFileName());
        } catch (IOException e) {
            view.showError("Could not export", "The bills could not be exported:\n" + e.getMessage());
        }
    }

    @Override
    public void onLogout() {
        if (!confirmDiscardIfDirty()) return;
        detach();
        view.dispose();
        logoutAction.run();
    }

    @Override
    public void onExit() {
        if (!confirmDiscardIfDirty()) return;
        detach();
        view.dispose();
        System.exit(0);
    }

    // ---------- helpers ----------

    private Bill selectedBill() {
        int row = view.selectedRow();
        if (row < 0) {
            view.showError("Select a bill in the Saved bills table first.");
            return null;
        }
        return repository.get(row);
    }

    private boolean isFormDirty() {
        return !view.guestName().isBlank() || !view.nightsText().isBlank() || !view.extrasText().isBlank()
                || !view.paidText().isBlank() || !view.notesText().isBlank();
    }

    private boolean confirmDiscardIfDirty() {
        return !isFormDirty()
                || view.confirm("Unsaved entry", "The form has an entry that has not been saved. Discard it?");
    }

    private void detach() {
        repository.removeChangeListener(footerListener);
    }

    private void refreshFooter() {
        BillRepository.Totals t = repository.totals();
        if (t.count() == 0) {
            view.setFooter("No bills yet. Fill in the form above and press Save bill.");
            return;
        }
        view.setFooter(String.format("%d bill%s   |   Billed: %s   |   Collected: %s   |   Outstanding: %s",
                t.count(), t.count() == 1 ? "" : "s",
                money(t.billed()), money(t.collected()), money(t.outstanding())));
    }

    private static String balanceText(Bill b) {
        return b.balance().signum() < 0
                ? "Change due: " + money(b.balance().negate())
                : "Balance: " + money(b.balance());
    }
}