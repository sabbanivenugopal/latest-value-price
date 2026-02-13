import java.util.UUID;

public class BatchId {
    private final String id;

    public BatchId() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "BatchId{" + "id='" + id + '\'' + "}";
    }
}