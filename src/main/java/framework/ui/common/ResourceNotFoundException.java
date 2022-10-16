package framework.ui.common;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String s, ReflectiveOperationException e) {
        super(s,e);
    }

    public ResourceNotFoundException(ResourceNotFoundException e) {
        super(e);
    }
}
