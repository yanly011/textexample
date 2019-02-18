package au.unisa.erl.textparse.textexample.controller;

public class Context {
    private String originalContext;
    private String taggedContext;
    private String displayContext;

    public String getOriginalContext() {
        return originalContext;
    }

    public void setOriginalContext(String originalContext) {
        this.originalContext = originalContext;
    }

    public String getDisplayContext() {
        return displayContext;
    }

    public void setDisplayContext(String displayContext) {
        this.displayContext = displayContext;
    }

    public String getTaggedContext() {
        return taggedContext;
    }

    public void setTaggedContext(String taggedContext) {
        this.taggedContext = taggedContext;
    }
}
