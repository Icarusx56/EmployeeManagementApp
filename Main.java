import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Function;

public class Main extends Application {
    private final EmployeeManager mgr = new EmployeeManager();
    private final Map<String, Button> nav = new LinkedHashMap<>();
    private Stage stage;
    private Scene scene;
    private StackPane content;
    private String css;

    public static void main(String[] args) { launch(args); }

    @Override public void start(Stage s) {
        stage = s;
        css = Main.class.getResource("style.css").toExternalForm();
        scene = new Scene(loginRoot(), 1120, 720);
        scene.getStylesheets().add(css);
        stage.setTitle("Workforce - Employee Management");
        stage.setMinWidth(980); stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    // ================= animation helpers =================
    static void fadeIn(Node n, double delayMs) {
        n.setOpacity(0); n.setTranslateY(18);
        FadeTransition f = new FadeTransition(Duration.millis(380), n); f.setToValue(1);
        TranslateTransition t = new TranslateTransition(Duration.millis(380), n); t.setToY(0);
        t.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition p = new ParallelTransition(f, t);
        p.setDelay(Duration.millis(delayMs)); p.play();
    }

    static void shake(Node n) {
        Timeline t = new Timeline();
        double[] xs = {-10, 10, -8, 8, -4, 0};
        for (int i = 0; i < xs.length; i++)
            t.getKeyFrames().add(new KeyFrame(Duration.millis(50 * (i + 1)), new KeyValue(n.translateXProperty(), xs[i])));
        t.play();
    }

    static void scale(Node n, double to) {
        ScaleTransition s = new ScaleTransition(Duration.millis(120), n);
        s.setToX(to); s.setToY(to); s.play();
    }

    static Button btn(String text, String style) {
        Button b = new Button(text);
        b.getStyleClass().add(style);
        b.setOnMouseEntered(e -> scale(b, 1.04));
        b.setOnMouseExited(e -> scale(b, 1));
        return b;
    }

    private void swap(Parent root) { scene.setRoot(root); fadeIn(root, 0); root.setTranslateY(0); }

    private Optional<ButtonType> alert(Alert.AlertType type, String msg) {
        Alert a = type == Alert.AlertType.CONFIRMATION
                ? new Alert(type, msg, ButtonType.OK, ButtonType.CANCEL) : new Alert(type, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.initOwner(stage);
        a.getDialogPane().getStylesheets().add(css);
        return a.showAndWait();
    }

    // ================= login =================
    private Parent loginRoot() {
        Label title = new Label("Welcome back"); title.getStyleClass().add("h1");
        Label sub = new Label("Sign in to Employee Management"); sub.getStyleClass().add("muted");
        TextField user = new TextField(); user.setPromptText("Username");
        PasswordField pass = new PasswordField(); pass.setPromptText("Password");
        Label err = new Label(); err.getStyleClass().add("error");
        Button login = btn("Sign in", "btn-primary"); login.setMaxWidth(Double.MAX_VALUE);
        Button exit = btn("Exit", "btn-ghost"); exit.setMaxWidth(Double.MAX_VALUE);
        Label hint = new Label("Demo: admin / admin123"); hint.getStyleClass().add("muted");

        VBox card = new VBox(14, title, sub, user, pass, err, login, exit, hint);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(40)); card.setMaxWidth(380); card.setMaxHeight(Region.USE_PREF_SIZE);
        VBox.setMargin(sub, new Insets(0, 0, 10, 0));

        Runnable go = () -> {
            if (user.getText().trim().equals("admin") && pass.getText().equals("admin123")) swap(appRoot());
            else { err.setText("Incorrect username or password."); shake(card); }
        };
        login.setOnAction(e -> go.run());
        pass.setOnAction(e -> go.run());
        exit.setOnAction(e -> Platform.exit());

        StackPane root = new StackPane(card);
        root.getStyleClass().add("login-bg");
        fadeIn(card, 100);
        return root;
    }

    // ================= shell + navigation =================
    private Parent appRoot() {
        Label brand = new Label("Workforce"); brand.getStyleClass().add("brand");
        VBox side = new VBox(6, brand);
        side.getStyleClass().add("sidebar"); side.setPrefWidth(230); side.setMinWidth(230);
        VBox.setMargin(brand, new Insets(0, 0, 28, 8));
        nav.clear();
        for (String n : List.of("Dashboard", "Add Employee", "Employees", "Departments")) {
            Button b = new Button(n); b.getStyleClass().add("nav"); b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> go(n));
            nav.put(n, b); side.getChildren().add(b);
        }
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        Button out = new Button("Log out"); out.getStyleClass().add("nav"); out.setMaxWidth(Double.MAX_VALUE);
        out.setOnAction(e -> swap(loginRoot()));
        side.getChildren().addAll(spacer, out);
        for (int i = 0; i < side.getChildren().size(); i++) fadeIn(side.getChildren().get(i), 60 * i);

        content = new StackPane(); content.getStyleClass().add("page");
        BorderPane root = new BorderPane(content); root.setLeft(side);
        go("Dashboard");
        return root;
    }

    private void go(String name) {
        nav.forEach((k, b) -> {
            b.getStyleClass().remove("nav-active");
            if (k.equals(name)) b.getStyleClass().add("nav-active");
        });
        Node page = switch (name) {
            case "Dashboard" -> dashboard();
            case "Add Employee" -> form(null);
            case "Employees" -> employees();
            default -> departments();
        };
        show(page);
    }

    private void show(Node page) { content.getChildren().setAll(page); fadeIn(page, 0); }

    private VBox page(String title, String subtitle, Node... body) {
        Label t = new Label(title); t.getStyleClass().add("h1");
        Label s = new Label(subtitle); s.getStyleClass().add("muted");
        VBox v = new VBox(20, new VBox(4, t, s));
        v.getChildren().addAll(body);
        v.setPadding(new Insets(36));
        return v;
    }

    // ================= dashboard =================
    private Node dashboard() {
        List<Employee> all = mgr.getAll();
        long ft = all.stream().filter(e -> e instanceof FullTimeEmployee).count();
        double payroll = all.stream().mapToDouble(Employee::calculateSalary).sum();
        HBox stats = new HBox(18,
                stat("Total employees", all.size(), 0), stat("Full-time", ft, 80),
                stat("Part-time", all.size() - ft, 160), stat("Monthly payroll (EGP)", payroll, 240));
        stats.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        Button add = btn("Add employee", "btn-primary"); add.setOnAction(e -> go("Add Employee"));
        Button view = btn("View employees", "btn-ghost"); view.setOnAction(e -> go("Employees"));
        return page("Welcome back, Admin", "Here's an overview of your company today.", stats, new HBox(12, add, view));
    }

    private VBox stat(String label, double value, double delay) {
        Label val = new Label("0"); val.getStyleClass().add("stat-value");
        Transition t = new Transition() {
            { setCycleDuration(Duration.millis(1000)); setInterpolator(Interpolator.EASE_OUT); }
            @Override protected void interpolate(double f) { val.setText(String.format("%,.0f", value * f)); }
        };
        t.setDelay(Duration.millis(delay + 150)); t.play();
        Label l = new Label(label); l.getStyleClass().add("muted");
        VBox c = new VBox(6, l, val); c.getStyleClass().add("card"); c.setPadding(new Insets(22));
        fadeIn(c, delay);
        return c;
    }

    // ================= employees (view / search / filter / delete) =================
    private <T> TableColumn<Employee, T> col(String title, Function<Employee, T> f, double w) {
        TableColumn<Employee, T> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(f.apply(cd.getValue())));
        c.setPrefWidth(w);
        return c;
    }

    @SuppressWarnings("unchecked")
    private Node employees() {
        TextField search = new TextField(); search.setPromptText("Search by ID or name...");
        HBox.setHgrow(search, Priority.ALWAYS);
        ComboBox<String> dept = new ComboBox<>();
        dept.getItems().add("All departments"); dept.getItems().addAll(mgr.getDepartmentNames());
        dept.setValue("All departments");
        Button edit = btn("Edit", "btn-ghost"), del = btn("Delete", "btn-danger");

        TableView<Employee> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                col("ID", Employee::getId, 60), col("NAME", Employee::getName, 160),
                col("AGE", Employee::getAge, 60), col("DEPARTMENT", Employee::getDepartment, 130),
                col("POSITION", Employee::getPosition, 130), col("TYPE", Employee::getType, 90),
                col("SALARY (EGP)", e -> String.format("%,.2f", e.calculateSalary()), 120));
        table.setPlaceholder(new Label("No matching employees found."));
        ObservableList<Employee> data = FXCollections.observableArrayList();
        table.setItems(data);

        Runnable refresh = () -> data.setAll(mgr.filter(search.getText(),
                "All departments".equals(dept.getValue()) ? null : dept.getValue()));
        search.textProperty().addListener((o, a, b) -> refresh.run());
        dept.setOnAction(e -> refresh.run());
        refresh.run();

        edit.setOnAction(e -> {
            Employee sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { alert(Alert.AlertType.WARNING, "Please select an employee first."); return; }
            nav.values().forEach(b -> b.getStyleClass().remove("nav-active"));
            show(form(sel));
        });
        del.setOnAction(e -> {
            Employee sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { alert(Alert.AlertType.WARNING, "Please select an employee first."); return; }
            Optional<ButtonType> r = alert(Alert.AlertType.CONFIRMATION,
                    "Delete " + sel.getName() + " (ID " + sel.getId() + ")? This cannot be undone.");
            if (r.isPresent() && r.get() == ButtonType.OK) { mgr.deleteEmployee(sel.getId()); refresh.run(); }
        });

        VBox card = new VBox(14, new HBox(12, search, dept, edit, del), table);
        card.getStyleClass().add("card"); card.setPadding(new Insets(20));
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox p = page("Employees", "Search, filter, edit or remove employees.", card);
        VBox.setVgrow(card, Priority.ALWAYS);
        return p;
    }

    // ================= add / update form =================
    private int toInt(String s, String what) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { throw new IllegalArgumentException(what + " must be a valid whole number."); }
    }
    private double toNum(String s, String what) {
        try {
            double d = Double.parseDouble(s.trim());
            if (d < 0 || Double.isNaN(d) || Double.isInfinite(d)) throw new Exception();
            return d;
        } catch (Exception e) { throw new IllegalArgumentException(what + " must be a non-negative number."); }
    }
    private String req(String s, String what) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(what + " is required.");
        return s.trim();
    }
    private VBox field(String label, Node input) {
        Label l = new Label(label); l.getStyleClass().add("field-label");
        return new VBox(6, l, input);
    }

    private Node form(Employee edit) {
        boolean upd = edit != null;
        TextField id = new TextField(upd ? "" + edit.getId() : "" + mgr.nextEmployeeId());
        id.setDisable(upd);
        TextField name = new TextField(upd ? edit.getName() : "");
        TextField age = new TextField(upd ? "" + edit.getAge() : "");
        TextField pos = new TextField(upd ? edit.getPosition() : "");
        ComboBox<String> dept = new ComboBox<>(FXCollections.observableArrayList(mgr.getDepartmentNames()));
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("Full-Time", "Part-Time"));
        dept.setMaxWidth(Double.MAX_VALUE); type.setMaxWidth(Double.MAX_VALUE);
        dept.setPromptText("Select department");
        TextField a = new TextField(), b = new TextField();
        Label la = new Label(), lb = new Label();
        la.getStyleClass().add("field-label"); lb.getStyleClass().add("field-label");
        VBox boxA = new VBox(6, la, a), boxB = new VBox(6, lb, b);

        if (upd) {
            dept.setValue(edit.getDepartment()); type.setValue(edit.getType());
            if (edit instanceof FullTimeEmployee f) { a.setText("" + f.getBasicSalary()); b.setText("" + f.getBonus()); }
            else if (edit instanceof PartTimeEmployee p) { a.setText("" + p.getHourlyRate()); b.setText("" + p.getHoursWorked()); }
        } else type.setValue("Full-Time");

        Runnable relabel = () -> {
            boolean ft = "Full-Time".equals(type.getValue());
            la.setText(ft ? "BASIC SALARY (EGP)" : "HOURLY RATE (EGP)");
            lb.setText(ft ? "MONTHLY BONUS (EGP)" : "HOURS WORKED / MONTH");
        };
        relabel.run();
        type.valueProperty().addListener((o, x, y) -> { relabel.run(); fadeIn(boxA, 0); fadeIn(boxB, 70); });

        GridPane g = new GridPane(); g.setHgap(20); g.setVgap(18);
        ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(50);
        g.getColumnConstraints().addAll(cc, cc);
        g.add(field("Employee ID", id), 0, 0); g.add(field("Full name", name), 1, 0);
        g.add(field("Age", age), 0, 1);        g.add(field("Department", dept), 1, 1);
        g.add(field("Position", pos), 0, 2);   g.add(field("Employee type", type), 1, 2);
        g.add(boxA, 0, 3); g.add(boxB, 1, 3);

        Button save = btn(upd ? "Update employee" : "Add employee", "btn-primary");
        Button cancel = btn(upd ? "Cancel" : "Clear", "btn-ghost");
        cancel.setOnAction(e -> go(upd ? "Employees" : "Add Employee"));

        save.setOnAction(e -> {
            try {
                int eid = toInt(id.getText(), "Employee ID");
                if (eid <= 0) throw new IllegalArgumentException("Employee ID must be positive.");
                String n = req(name.getText(), "Full name");
                int ag = toInt(age.getText(), "Age");
                if (ag <= 0) throw new IllegalArgumentException("Age must be a positive whole number.");
                String d = dept.getValue();
                if (d == null) throw new IllegalArgumentException("Please select a department.");
                String p = req(pos.getText(), "Position");
                String t = type.getValue();
                if (t == null) throw new IllegalArgumentException("Please select an employee type.");
                boolean ft = t.equals("Full-Time");
                double v1 = toNum(a.getText(), ft ? "Basic salary" : "Hourly rate");
                double v2 = toNum(b.getText(), ft ? "Bonus" : "Hours worked");
                Employee emp = ft ? new FullTimeEmployee(eid, n, ag, d, p, v1, v2)
                                  : new PartTimeEmployee(eid, n, ag, d, p, v1, v2);
                if (upd) {
                    mgr.updateEmployee(emp);
                    alert(Alert.AlertType.INFORMATION, "Employee updated successfully!");
                    go("Employees");
                } else if (!mgr.addEmployee(emp)) {
                    alert(Alert.AlertType.ERROR, "Employee ID " + eid + " already exists.");
                } else {
                    alert(Alert.AlertType.INFORMATION, "Employee added successfully!");
                    go("Add Employee");
                }
            } catch (IllegalArgumentException ex) {
                alert(Alert.AlertType.ERROR, ex.getMessage());
            }
        });

        VBox card = new VBox(26, g, new HBox(12, save, cancel));
        card.getStyleClass().add("card"); card.setPadding(new Insets(30)); card.setMaxWidth(760);
        return page(upd ? "Update employee" : "Add employee",
                upd ? "Edit the details below and save." : "Fill in the details to register a new employee.", card);
    }

    // ================= departments =================
    private Node departments() {
        ListView<String> list = new ListView<>();
        Runnable refresh = () -> list.getItems().setAll(mgr.getDepartments().stream().map(d ->
                String.format("#%d     %s     (%d employees)", d.getId(), d.getName(),
                        mgr.filter("", d.getName()).size())).toList());
        refresh.run();
        VBox listCard = new VBox(list); listCard.getStyleClass().add("card"); listCard.setPadding(new Insets(10));
        VBox.setVgrow(list, Priority.ALWAYS);
        HBox.setHgrow(listCard, Priority.ALWAYS);

        TextField id = new TextField("" + mgr.nextDepartmentId());
        TextField name = new TextField(); name.setPromptText("e.g. Operations");
        Button add = btn("Add department", "btn-primary"); add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(e -> {
            try {
                String err = mgr.addDepartment(toInt(id.getText(), "Department ID"), name.getText());
                if (err != null) { alert(Alert.AlertType.ERROR, err); return; }
                name.clear(); id.setText("" + mgr.nextDepartmentId()); refresh.run();
            } catch (IllegalArgumentException ex) { alert(Alert.AlertType.ERROR, ex.getMessage()); }
        });
        VBox formCard = new VBox(18, field("Department ID", id), field("Department name", name), add);
        formCard.getStyleClass().add("card"); formCard.setPadding(new Insets(24));
        formCard.setPrefWidth(300); formCard.setMinWidth(300); formCard.setMaxHeight(Region.USE_PREF_SIZE);

        HBox row = new HBox(20, listCard, formCard);
        VBox p = page("Departments", "Manage the departments employees can be assigned to.", row);
        VBox.setVgrow(row, Priority.ALWAYS);
        return p;
    }
}
