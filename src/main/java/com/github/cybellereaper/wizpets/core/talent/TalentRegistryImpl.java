package com.github.cybellereaper.wizpets.core.talent;

import com.github.cybellereaper.wizpets.api.talent.PetTalent;
import com.github.cybellereaper.wizpets.api.talent.PetTalentDescriptor;
import com.github.cybellereaper.wizpets.api.talent.TalentFactory;
import com.github.cybellereaper.wizpets.api.talent.TalentRegistryView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class TalentRegistryImpl implements TalentRegistryView {
  private final Map<String, TalentFactoryWrapper> factories = new LinkedHashMap<>();
  private final Object monitor = new Object();

  public void register(TalentFactory factory) {
    register(factory, false);
  }

  public void register(TalentFactory factory, boolean replace) {
    Objects.requireNonNull(factory, "factory");
    PetTalent sample = factory.create();
    String id = sample.getId();
    synchronized (monitor) {
      if (!replace && factories.containsKey(id)) {
        throw new IllegalArgumentException("Talent with id " + id + " already registered");
      }
      factories.put(id, new TalentFactoryWrapper(sample, factory));
    }
  }

  public void unregister(String id) {
    synchronized (monitor) {
      factories.remove(id);
    }
  }

  public List<PetTalent> instantiate(List<String> ids) {
    synchronized (monitor) {
      List<PetTalent> talents = new ArrayList<>(ids.size());
      for (String id : ids) {
        TalentFactoryWrapper wrapper = factories.get(id);
        if (wrapper != null) {
          talents.add(wrapper.create());
        }
      }
      return List.copyOf(talents);
    }
  }

  public List<String> roll(RandomGenerator random) {
    return roll(random, 2);
  }

  public List<String> roll(RandomGenerator random, int count) {
    Objects.requireNonNull(random, "random");
    synchronized (monitor) {
      if (factories.isEmpty() || count <= 0) {
        return Collections.emptyList();
      }
      List<String> keys = new ArrayList<>(factories.keySet());
      List<String> result = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
        result.add(keys.get(random.nextInt(keys.size())));
      }
      return List.copyOf(result);
    }
  }

  public List<String> inherit(List<String> parentA, List<String> parentB, RandomGenerator random) {
    return inherit(parentA, parentB, random, 2);
  }

  public List<String> inherit(
      List<String> parentA, List<String> parentB, RandomGenerator random, int count) {
    Objects.requireNonNull(parentA, "parentA");
    Objects.requireNonNull(parentB, "parentB");
    Objects.requireNonNull(random, "random");
    synchronized (monitor) {
      if (count <= 0) {
        return Collections.emptyList();
      }
      LinkedHashSet<String> pool = new LinkedHashSet<>(parentA);
      pool.addAll(parentB);
      if (pool.isEmpty()) {
        return roll(random, count);
      }
      List<String> ordered = new ArrayList<>(pool);
      shuffle(ordered, random);
      List<String> chosen = new ArrayList<>(ordered.subList(0, Math.min(count, ordered.size())));
      while (chosen.size() < count) {
        chosen.addAll(roll(random, 1));
      }
      return List.copyOf(chosen.subList(0, count));
    }
  }

  @Override
  public Iterator<PetTalentDescriptor> iterator() {
    synchronized (monitor) {
      List<PetTalentDescriptor> descriptors = new ArrayList<>(factories.size());
      factories.values().forEach(wrapper -> descriptors.add(wrapper.descriptor));
      return descriptors.iterator();
    }
  }

  @Override
  public PetTalentDescriptor get(String id) {
    synchronized (monitor) {
      TalentFactoryWrapper wrapper = factories.get(id);
      return wrapper != null ? wrapper.descriptor : null;
    }
  }

  @Override
  public int size() {
    synchronized (monitor) {
      return factories.size();
    }
  }

  private static void shuffle(List<String> values, RandomGenerator random) {
    for (int i = values.size() - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      Collections.swap(values, i, j);
    }
  }

  private static final class TalentFactoryWrapper implements TalentFactory {
    private final PetTalentDescriptor descriptor;
    private final TalentFactory delegate;

    private TalentFactoryWrapper(PetTalent sample, TalentFactory delegate) {
      this.descriptor =
          new PetTalentDescriptor(sample.getId(), sample.getDisplayName(), sample.getDescription());
      this.delegate = delegate;
    }

    @Override
    public PetTalent create() {
      return delegate.create();
    }
  }
}
