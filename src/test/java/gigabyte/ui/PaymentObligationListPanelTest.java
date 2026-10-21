package gigabyte.ui;

import static gigabyte.testutil.TypicalClients.ALICE;
import static gigabyte.testutil.TypicalClients.BENSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gigabyte.commons.core.index.Index;
import gigabyte.logic.commands.EditPaymentCommand;
import gigabyte.model.ModelManager;
import gigabyte.model.gig.Deadline;
import gigabyte.model.gig.Fee;
import gigabyte.model.gig.Gig;
import gigabyte.model.gig.GigStatus;
import gigabyte.model.gig.GigTitle;
import gigabyte.model.gig.PaymentObligation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollBar;

public class PaymentObligationListPanelTest {
    private static final Clock TEST_CLOCK = Clock.fixed(Instant.parse("2026-10-11T04:00:00Z"),
            ZoneId.of("Asia/Kuala_Lumpur"));
    private static final Gig FIRST_GIG = createGig(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final Gig FIELD_IDENTICAL_GIG = createGig(
            UUID.fromString("00000000-0000-0000-0000-000000000002"));

    @BeforeAll
    public static void setUpJavaFx() {
        JavaFxTestUtil.startToolkit();
    }

    @Test
    public void constructor_noSelectedGig_showsNoGigEmptyState() {
        PaymentObligationListPanel panel = createPanel(FXCollections.observableArrayList());

        assertEquals(0, panel.getPaymentListView().getItems().size());
        assertEquals("Select a gig to view its payment obligations.", panel.getEmptyState().getText());
    }

    @Test
    public void showOutstandingPayments_multipleClients_sortsExcludesPaidAndKeepsStableTies() {
        Gig otherGig = new Gig(BENSON, new GigTitle("Logo"), GigStatus.COMPLETED,
                new Deadline("2027-01-31"), new Fee("500"));
        PaymentObligation future = createPayment(otherGig, "100", "2026-10-12", false);
        PaymentObligation today = createPayment(FIRST_GIG, "200", "2026-10-11", false);
        PaymentObligation paid = createPayment(otherGig, "300", "2026-10-09", true);
        PaymentObligation overdue = createPayment(FIRST_GIG, "400", "2026-10-10", false);
        PaymentObligation sameDate = createPayment(otherGig, "500", "2026-10-11", false);
        PaymentObligation identical = createPayment(FIRST_GIG, "200", "2026-10-11", false);
        List<PaymentObligation> original = List.of(future, today, paid, overdue, identical, sameDate);
        ObservableList<PaymentObligation> payments = FXCollections.observableArrayList(original);
        PaymentObligationListPanel panel = createPanel(payments);

        panel.showOutstandingPayments();

        assertEquals(List.of(overdue, today, identical, sameDate, future), panel.getPaymentListView().getItems());
        assertSame(today, panel.getPaymentListView().getItems().get(1));
        assertSame(identical, panel.getPaymentListView().getItems().get(2));
        assertEquals(original, payments);
        assertEquals("", panel.getEmptyState().getText());
        new Scene(panel.getRoot(), 500, 500);
        List<ListCell<PaymentObligation>> cells = renderCells(panel);
        assertTrue(cells.get(0).getText().contains("OVERDUE"));
        assertTrue(cells.get(0).getStyleClass().contains("payment-overdue"));
        for (int i = 1; i < cells.size(); i++) {
            assertTrue(cells.get(i).getText().contains("OUTSTANDING"));
            assertFalse(cells.get(i).getStyleClass().contains("payment-overdue"));
        }
        assertTrue(cells.get(3).getText().contains("Client: " + BENSON.getName()));
        assertTrue(cells.get(3).getText().contains("Gig: Logo"));
        assertTrue(cells.get(3).getText().contains("Amount: $500.00"));
        assertTrue(cells.get(3).getText().contains("Due: 2026-10-11"));
    }

    @Test
    public void showOutstandingPayments_paidStateChanges_refreshesAndRestoresGigView() {
        ObservableList<PaymentObligation> payments = FXCollections.observableArrayList();
        PaymentObligationListPanel panel = createPanel(payments);
        panel.showOutstandingPayments();
        assertEquals("No outstanding payment obligations.", panel.getEmptyState().getText());
        PaymentObligation unpaid = createPayment(FIRST_GIG, "100", "2026-10-11", false);
        payments.add(unpaid);
        assertEquals(List.of(unpaid), panel.getPaymentListView().getItems());

        payments.set(0, unpaid.withPaid(true));
        panel.showOutstandingPayments();
        assertEquals(0, panel.getPaymentListView().getItems().size());
        assertEquals("No outstanding payment obligations.", panel.getEmptyState().getText());

        panel.showPaymentsFor(FIRST_GIG);
        assertEquals(payments, panel.getPaymentListView().getItems());
        assertTrue(renderCells(panel).get(0).getText().contains("PAID"));
        panel.showPaymentsFor(null);
        assertEquals("Select a gig to view its payment obligations.", panel.getEmptyState().getText());
    }

    @Test
    public void showOutstandingPayments_localMidnight_usesLocalDateAndRefreshesOverdueState() {
        MutableClock localClock = new MutableClock(Instant.parse("2026-10-10T16:00:00Z"),
                ZoneId.of("Asia/Kuala_Lumpur"));
        PaymentObligation payment = createPayment(FIRST_GIG, "100", "2026-10-11", false);
        PaymentObligationListPanel panel = new PaymentObligationListPanel(
                FXCollections.observableArrayList(payment), localClock);
        panel.showOutstandingPayments();
        assertTrue(renderCells(panel).get(0).getText().contains("OUTSTANDING"));

        localClock.advance(Duration.ofDays(1));
        panel.refreshPaymentStates();
        assertTrue(renderCells(panel).get(0).getText().contains("OVERDUE"));
        assertFalse(payment.isPaid());
    }

    @Test
    public void showPaymentsFor_fieldIdenticalGigs_filtersByStableUid() {
        PaymentObligation firstGigPayment = createPayment(FIRST_GIG, "100", "2027-01-01", false);
        PaymentObligation secondGigPayment = createPayment(FIELD_IDENTICAL_GIG, "200", "2027-01-01", false);
        PaymentObligationListPanel panel = createPanel(
                FXCollections.observableArrayList(firstGigPayment, secondGigPayment));

        panel.showPaymentsFor(FIRST_GIG);

        assertEquals(1, panel.getPaymentListView().getItems().size());
        assertSame(firstGigPayment, panel.getPaymentListView().getItems().get(0));
    }

    @Test
    public void showPaymentsFor_multiplePayments_sortsByDueDateThenInsertionOrder() {
        PaymentObligation laterPayment = createPayment(FIRST_GIG, "300", "2027-03-01", false);
        PaymentObligation firstEqualDatePayment = createPayment(FIRST_GIG, "200", "2027-02-01", false);
        PaymentObligation secondEqualDatePayment = createPayment(FIRST_GIG, "100", "2027-02-01", false);
        PaymentObligation earliestPayment = createPayment(FIRST_GIG, "400", "2027-01-01", false);
        PaymentObligationListPanel panel = createPanel(FXCollections.observableArrayList(
                laterPayment, firstEqualDatePayment, secondEqualDatePayment, earliestPayment));

        panel.showPaymentsFor(FIRST_GIG);

        assertEquals(List.of(earliestPayment, firstEqualDatePayment, secondEqualDatePayment, laterPayment),
                panel.getPaymentListView().getItems());
    }

    @Test
    public void showPaymentsFor_paymentAdded_refreshesImmediately() {
        ObservableList<PaymentObligation> payments = FXCollections.observableArrayList();
        PaymentObligationListPanel panel = createPanel(payments);
        panel.showPaymentsFor(FIRST_GIG);
        PaymentObligation addedPayment = createPayment(FIRST_GIG, "500", "2027-01-01", false);

        payments.add(addedPayment);

        assertEquals(List.of(addedPayment), panel.getPaymentListView().getItems());
        assertEquals("", panel.getEmptyState().getText());
    }

    @Test
    public void editPaymentCommand_dueDateCrossesToday_reordersAndRefreshesGigView() throws Exception {
        ModelManager model = createModelWithFirstGig();
        Gig storedGig = model.getGigList().get(0);
        model.addPaymentObligation(createPayment(storedGig, "100", "2026-10-12", false));
        model.addPaymentObligation(createPayment(storedGig, "200", "2026-10-11", false));
        PaymentObligationListPanel panel = createPanel(model.getPaymentObligationList());
        panel.showPaymentsFor(storedGig);
        assertEquals(new Fee("200"), panel.getPaymentListView().getItems().get(0).getAmount());

        new EditPaymentCommand(Index.fromOneBased(1), Index.fromOneBased(1), Index.fromOneBased(2),
                Optional.empty(), Optional.of(new Deadline("2026-10-10"))).execute(model);

        assertEquals(new Fee("100"), panel.getPaymentListView().getItems().get(0).getAmount());
        List<ListCell<PaymentObligation>> cells = renderCells(panel);
        assertTrue(cells.get(0).getText().contains("Due: 2026-10-10"));
        assertTrue(cells.get(0).getStyleClass().contains("payment-overdue"));
        assertTrue(cells.get(1).getStyleClass().contains("payment-outstanding"));

        new EditPaymentCommand(Index.fromOneBased(1), Index.fromOneBased(1), Index.fromOneBased(1),
                Optional.empty(), Optional.of(new Deadline("2026-10-12"))).execute(model);

        assertEquals(new Fee("200"), panel.getPaymentListView().getItems().get(0).getAmount());
        cells = renderCells(panel);
        assertTrue(cells.get(1).getText().contains("Due: 2026-10-12"));
        assertTrue(cells.get(1).getStyleClass().contains("payment-outstanding"));
    }

    @Test
    public void editPaymentCommand_amountChanges_refreshesOutstandingViewImmediately() throws Exception {
        ModelManager model = createModelWithFirstGig();
        Gig storedGig = model.getGigList().get(0);
        model.addPaymentObligation(createPayment(storedGig, "100", "2026-10-12", false));
        PaymentObligationListPanel panel = createPanel(model.getPaymentObligationList());
        panel.showOutstandingPayments();

        new EditPaymentCommand(Index.fromOneBased(1), Index.fromOneBased(1), Index.fromOneBased(1),
                Optional.of(new Fee("650")), Optional.empty()).execute(model);

        assertEquals(new Fee("650"), panel.getPaymentListView().getItems().get(0).getAmount());
        assertTrue(renderCells(panel).get(0).getText().contains("Amount: $650.00"));
    }

    @Test
    public void editPaymentCommand_dueDateChanges_reordersAndRefreshesOutstandingView() throws Exception {
        ModelManager model = createModelWithFirstGig();
        Gig storedGig = model.getGigList().get(0);
        model.addPaymentObligation(createPayment(storedGig, "100", "2026-10-12", false));
        model.addPaymentObligation(createPayment(storedGig, "200", "2026-10-11", false));
        PaymentObligationListPanel panel = createPanel(model.getPaymentObligationList());
        panel.showOutstandingPayments();
        assertEquals(new Fee("200"), panel.getPaymentListView().getItems().get(0).getAmount());

        new EditPaymentCommand(Index.fromOneBased(1), Index.fromOneBased(1), Index.fromOneBased(2),
                Optional.empty(), Optional.of(new Deadline("2026-10-10"))).execute(model);

        assertEquals(new Fee("100"), panel.getPaymentListView().getItems().get(0).getAmount());
        List<ListCell<PaymentObligation>> cells = renderCells(panel);
        assertTrue(cells.get(0).getText().contains("Due: 2026-10-10"));
        assertTrue(cells.get(0).getText().contains("OVERDUE"));
        assertTrue(cells.get(0).getStyleClass().contains("payment-overdue"));

        new EditPaymentCommand(Index.fromOneBased(1), Index.fromOneBased(1), Index.fromOneBased(1),
                Optional.empty(), Optional.of(new Deadline("2026-10-12"))).execute(model);

        assertEquals(new Fee("200"), panel.getPaymentListView().getItems().get(0).getAmount());
        cells = renderCells(panel);
        assertTrue(cells.get(1).getText().contains("Due: 2026-10-12"));
        assertTrue(cells.get(1).getText().contains("OUTSTANDING"));
        assertTrue(cells.get(1).getStyleClass().contains("payment-outstanding"));
    }

    @Test
    public void showPaymentsFor_gigWithoutPayments_showsNoPaymentsEmptyState() {
        PaymentObligationListPanel panel = createPanel(FXCollections.observableArrayList());

        panel.showPaymentsFor(FIRST_GIG);

        assertEquals("This gig has no payment obligations.", panel.getEmptyState().getText());
    }

    @Test
    public void paymentCell_renderedPayments_showsIndexFieldsAndStateStyles() {
        PaymentObligation paidPayment = createPayment(FIRST_GIG, "100", "2026-10-09", true);
        PaymentObligation overduePayment = createPayment(FIRST_GIG, "200", "2026-10-10", false);
        PaymentObligation outstandingPayment = createPayment(FIRST_GIG, "300", "2026-10-11", false);
        PaymentObligationListPanel panel = createPanel(
                FXCollections.observableArrayList(outstandingPayment, overduePayment, paidPayment));
        panel.showPaymentsFor(FIRST_GIG);

        List<ListCell<PaymentObligation>> cells = renderCells(panel);

        assertTrue(cells.get(0).getText().contains("1. Amount: $100.00"));
        assertTrue(cells.get(0).getText().contains("Due: 2026-10-09"));
        assertTrue(cells.get(0).getText().contains("PAID"));
        assertTrue(cells.get(0).getStyleClass().contains("payment-paid"));
        assertTrue(cells.get(1).getText().contains("2. Amount: $200.00"));
        assertTrue(cells.get(1).getText().contains("OVERDUE"));
        assertTrue(cells.get(1).getStyleClass().contains("payment-overdue"));
        assertTrue(cells.get(2).getText().contains("3. Amount: $300.00"));
        assertTrue(cells.get(2).getText().contains("OUTSTANDING"));
        assertTrue(cells.get(2).getStyleClass().contains("payment-outstanding"));
    }

    @Test
    public void paymentCell_turkishDefaultLocale_usesLocaleIndependentStateStyle() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            PaymentObligation paidPayment = createPayment(FIRST_GIG, "100", "2026-10-09", true);
            PaymentObligationListPanel panel = createPanel(FXCollections.observableArrayList(paidPayment));
            panel.showPaymentsFor(FIRST_GIG);

            List<ListCell<PaymentObligation>> cells = renderCells(panel);

            assertTrue(cells.get(0).getStyleClass().contains("payment-paid"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void refreshPaymentStates_localDateAdvances_updatesOutstandingToOverdue() {
        MutableClock mutableClock = new MutableClock(Instant.parse("2026-10-11T04:00:00Z"),
                ZoneId.of("Asia/Kuala_Lumpur"));
        PaymentObligation payment = createPayment(FIRST_GIG, "100", "2026-10-11", false);
        PaymentObligationListPanel panel = new PaymentObligationListPanel(
                FXCollections.observableArrayList(payment), mutableClock);
        panel.showPaymentsFor(FIRST_GIG);
        assertTrue(renderCells(panel).get(0).getText().contains("OUTSTANDING"));

        mutableClock.advance(Duration.ofDays(1));
        panel.refreshPaymentStates();

        assertTrue(renderCells(panel).get(0).getText().contains("OVERDUE"));
    }

    @Test
    public void paymentCell_narrowPanel_wrapsWithoutHorizontalScrolling() {
        PaymentObligation payment = createPayment(FIRST_GIG, "500", "2099-02-28", false);
        ObservableList<PaymentObligation> payments = FXCollections.observableArrayList(payment);
        for (int i = 0; i < 29; i++) {
            payments.add(createPayment(FIELD_IDENTICAL_GIG, "100", "2099-03-01", false));
        }
        PaymentObligationListPanel panel = createPanel(payments);
        panel.showOutstandingPayments();

        new Scene(panel.getRoot(), 340, 300);
        List<ListCell<PaymentObligation>> cells = renderCells(panel);

        assertEquals(30, panel.getPaymentListView().getItems().size());
        assertFalse(cells.isEmpty());
        ListCell<PaymentObligation> cell = cells.get(0);
        assertTrue(cell.isWrapText());
        assertTrue(cell.getWidth() <= panel.getPaymentListView().getWidth());
        assertTrue(cell.getText().contains("Amount: $500.00"));
        assertTrue(cell.getText().contains("Due: 2099-02-28"));
        assertTrue(cell.getText().contains("OUTSTANDING"));
        assertTrue(cell.getText().contains("Client: " + ALICE.getName()));
        assertTrue(cell.getText().contains("Gig: " + FIRST_GIG.getTitle()));

        boolean hasVisibleHorizontalScrollBar = panel.getPaymentListView()
                .lookupAll(".scroll-bar").stream()
                .anyMatch(node -> node instanceof ScrollBar scrollBar
                        && scrollBar.getOrientation() == Orientation.HORIZONTAL
                        && scrollBar.isVisible());

        assertFalse(hasVisibleHorizontalScrollBar);
        assertTrue(panel.getPaymentListView().lookupAll(".scroll-bar").stream()
                .anyMatch(node -> node instanceof ScrollBar bar
                        && bar.getOrientation() == Orientation.VERTICAL && bar.isVisible()));
    }

    private PaymentObligationListPanel createPanel(ObservableList<PaymentObligation> payments) {
        return new PaymentObligationListPanel(payments, TEST_CLOCK);
    }

    private ModelManager createModelWithFirstGig() {
        ModelManager model = new ModelManager();
        model.addClient(ALICE);
        model.addGig(FIRST_GIG);
        return model;
    }

    @SuppressWarnings("unchecked")
    private List<ListCell<PaymentObligation>> renderCells(PaymentObligationListPanel panel) {
        if (panel.getRoot().getScene() == null) {
            new Scene(panel.getRoot(), 500, 300);
        }
        panel.getRoot().applyCss();
        panel.getRoot().layout();
        return panel.getPaymentListView().lookupAll(".list-cell").stream()
                .map(node -> (ListCell<PaymentObligation>) node)
                .filter(cell -> !cell.isEmpty())
                .sorted(Comparator.comparingInt((ListCell<PaymentObligation> cell) -> cell.getIndex()))
                .toList();
    }

    private static Gig createGig(UUID uid) {
        return new Gig(uid, ALICE, new GigTitle("Website redesign"), GigStatus.IN_PROGRESS,
                new Deadline("2027-12-31"), new Fee("1000"));
    }

    private static PaymentObligation createPayment(Gig gig, String amount, String dueDate, boolean isPaid) {
        return new PaymentObligation(gig, new Fee(amount), new Deadline(dueDate), isPaid);
    }

    private static class MutableClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zoneId;

        MutableClock(Instant currentInstant, ZoneId zoneId) {
            this.currentInstant = currentInstant;
            this.zoneId = zoneId;
        }

        void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}
