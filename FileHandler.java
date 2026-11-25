import java.io.*;
import java.util.ArrayList;

/** Saves/loads the real Employee and Department objects via Java serialization. */
public class FileHandler {
    private static final String EMP_FILE = "employees.dat";
    private static final String DEP_FILE = "departments.dat";

    @SuppressWarnings("unchecked")
    private static <T> ArrayList<T> load(String path) {
        File f = new File(path);
        if (!f.exists()) return new ArrayList<>();           // first launch
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            return (ArrayList<T>) in.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            System.err.println("Could not read " + path + ": " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    private static boolean save(String path, ArrayList<?> data) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path))) {
            out.writeObject(data);
            return true;
        } catch (IOException ex) {
            System.err.println("Could not write " + path + ": " + ex.getMessage());
            return false;
        }
    }

    public static ArrayList<Employee> loadEmployees() { return load(EMP_FILE); }
    public static ArrayList<Department> loadDepartments() { return load(DEP_FILE); }
    public static boolean saveEmployees(ArrayList<Employee> l) { return save(EMP_FILE, l); }
    public static boolean saveDepartments(ArrayList<Department> l) { return save(DEP_FILE, l); }
}
