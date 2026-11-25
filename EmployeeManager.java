import java.util.ArrayList;
import java.util.List;

public class EmployeeManager {
    private ArrayList<Employee> employees = FileHandler.loadEmployees();
    private ArrayList<Department> departments = FileHandler.loadDepartments();

    public EmployeeManager() {
        if (departments.isEmpty()) {
            String[] defaults = {"IT", "Human Resources", "Finance", "Marketing", "Sales"};
            for (int i = 0; i < defaults.length; i++) departments.add(new Department(i + 1, defaults[i]));
            FileHandler.saveDepartments(departments);
        }
    }

    // ---------- employees ----------
    public boolean addEmployee(Employee e) {
        if (e == null || searchEmployee(e.getId()) != null) return false;
        employees.add(e);
        FileHandler.saveEmployees(employees);
        return true;
    }

    public Employee searchEmployee(int id) {
        for (Employee e : employees) if (e.getId() == id) return e;
        return null;
    }

    public boolean updateEmployee(Employee u) {
        for (int i = 0; i < employees.size(); i++) {
            if (employees.get(i).getId() == u.getId()) {
                employees.set(i, u);
                FileHandler.saveEmployees(employees);
                return true;
            }
        }
        return false;
    }

    public boolean deleteEmployee(int id) {
        Employee e = searchEmployee(id);
        if (e == null) return false;
        employees.remove(e);
        FileHandler.saveEmployees(employees);
        return true;
    }

    public List<Employee> getAll() { return new ArrayList<>(employees); }

    /** query matches ID exactly or name (case-insensitive); dept null/blank = all. */
    public List<Employee> filter(String query, String dept) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Employee> out = new ArrayList<>();
        for (Employee e : employees) {
            boolean okDept = dept == null || dept.isBlank() || e.getDepartment().equalsIgnoreCase(dept);
            boolean okQuery = q.isEmpty() || String.valueOf(e.getId()).equals(q)
                    || e.getName().toLowerCase().contains(q);
            if (okDept && okQuery) out.add(e);
        }
        return out;
    }

    public int nextEmployeeId() {
        int max = 0;
        for (Employee e : employees) max = Math.max(max, e.getId());
        return max + 1;
    }

    // ---------- departments ----------
    public List<Department> getDepartments() { return new ArrayList<>(departments); }

    public List<String> getDepartmentNames() {
        List<String> n = new ArrayList<>();
        for (Department d : departments) n.add(d.getName());
        return n;
    }

    public int nextDepartmentId() {
        int max = 0;
        for (Department d : departments) max = Math.max(max, d.getId());
        return max + 1;
    }

    /** Returns an error message, or null on success. */
    public String addDepartment(int id, String name) {
        if (name == null || name.isBlank()) return "Department name is required.";
        if (id <= 0) return "Department ID must be a positive whole number.";
        for (Department d : departments) {
            if (d.getId() == id) return "Department ID " + id + " already exists.";
            if (d.getName().equalsIgnoreCase(name.trim())) return "A department with this name already exists.";
        }
        departments.add(new Department(id, name.trim()));
        FileHandler.saveDepartments(departments);
        return null;
    }
}
