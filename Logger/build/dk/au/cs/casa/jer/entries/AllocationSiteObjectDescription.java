package dk.au.cs.casa.jer.entries;

public class AllocationSiteObjectDescription extends ObjectDescription {

    private final SourceLocation allocationSite;

    public AllocationSiteObjectDescription(SourceLocation allocationSite) {
        this.allocationSite = allocationSite;
    }

    public SourceLocation getAllocationSite() {
        return allocationSite;
    }

    @Override
    public <T> T accept(ObjectDescriptionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "AllocationSite(" + allocationSite + ")";
    }
}
