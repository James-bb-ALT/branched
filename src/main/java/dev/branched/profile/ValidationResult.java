package dev.branched.profile;

import dev.branched.core.ResourceId;
import dev.branched.species.SpeciesProfile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Validation outcome for one profile file. */
public final class ValidationResult {
    private final ResourceId source;
    private final SpeciesProfile profile;
    private final List<ValidationIssue> issues;

    public ValidationResult(ResourceId source, SpeciesProfile profile, List<ValidationIssue> issues) {
        this.source = source;
        this.profile = profile;
        this.issues = List.copyOf(issues);
    }

    public ResourceId source() { return source; }
    public Optional<SpeciesProfile> profile() { return Optional.ofNullable(profile); }
    public List<ValidationIssue> issues() { return issues; }

    public boolean ok() {
        return profile != null && issues.stream().noneMatch(i -> i.severity() == ValidationIssue.Severity.ERROR);
    }

    public List<ValidationIssue> errors() {
        return issues.stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).collect(Collectors.toList());
    }

    public List<ValidationIssue> warnings() {
        return issues.stream().filter(i -> i.severity() == ValidationIssue.Severity.WARNING).collect(Collectors.toList());
    }

    public String format() {
        return source + ": " + (ok() ? "OK" : "REJECTED") + (issues.isEmpty() ? "" : " | " + issues);
    }
}
