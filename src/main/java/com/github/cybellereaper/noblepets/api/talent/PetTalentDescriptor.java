package com.github.cybellereaper.noblepets.api.talent;

import java.util.Objects;

public final class PetTalentDescriptor {
  private final String id;
  private final String displayName;
  private final String description;

  public PetTalentDescriptor(String id, String displayName, String description) {
    this.id = Objects.requireNonNull(id, "id");
    this.displayName = Objects.requireNonNull(displayName, "displayName");
    this.description = Objects.requireNonNull(description, "description");
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PetTalentDescriptor that)) {
      return false;
    }
    return id.equals(that.id)
        && displayName.equals(that.displayName)
        && description.equals(that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, displayName, description);
  }

  @Override
  public String toString() {
    return "PetTalentDescriptor{"
        + "id='"
        + id
        + '\''
        + ", displayName='"
        + displayName
        + '\''
        + ", description='"
        + description
        + '\''
        + '}';
  }
}
