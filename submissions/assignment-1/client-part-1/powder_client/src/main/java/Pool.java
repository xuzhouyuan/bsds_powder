import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Pool<T> {
    protected Set<T> registered;

    public Pool() {
        this.registered = new HashSet<T>();
        System.out.println(this.registered.getClass().getName());
    }

    public Pool(List<T> candidates) {
        this();
        addAll(candidates);
    }

    public int size() {
        return registered.size();
    }

    public boolean contains(T candidate) {
        return registered.contains(candidate);
    }

    public void add(T candidate) {
        registered.add(candidate);
    }

    public void addAll(List<T> candidates) {
        registered.addAll(candidates);
    }

    public void remove(T candidate) {
        registered.remove(candidate);
    }
}

class IdPool extends Pool<Integer> {
    public IdPool() {
        this.registered = new HashSet<Integer>();
    }

    public IdPool(int start, int end) {
        this();
        addRange(start, end);
    }

    public void addRange(int start, int end) {
        for (int i = start; i < end; i++) {
            this.registered.add(i);
        }
    }

    public void removeRange(int start, int end) {
        for (int i = start; i < end; i++) {
            this.registered.remove(i);
        }
    }
}
