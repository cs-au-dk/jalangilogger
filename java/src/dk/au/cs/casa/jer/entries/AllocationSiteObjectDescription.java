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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AllocationSiteObjectDescription that = (AllocationSiteObjectDescription) o;

        return allocationSite != null ? allocationSite.equals(that.allocationSite) : that.allocationSite == null;
    }

    @Override
    public int hashCode() {
        return allocationSite != null ? allocationSite.hashCode() : 0;
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
