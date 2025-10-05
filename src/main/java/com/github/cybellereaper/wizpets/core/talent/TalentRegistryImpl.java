package com.github.cybellereaper.wizpets.core.talent;

import com.github.cybellereaper.wizpets.api.talent.PetTalent;
import com.github.cybellereaper.wizpets.api.talent.PetTalentDescriptor;
import com.github.cybellereaper.wizpets.api.talent.TalentFactory;
import com.github.cybellereaper.wizpets.api.talent.TalentRegistryView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public final class TalentRegistryImpl implements TalentRegistryView {
    private final Map<String, TalentFactoryWrapper> factories = new LinkedHashMap<>();

    public void register(TalentFactory factory) {
        register(factory, false);
    }

    public void register(TalentFactory factory, boolean replace) {
        Objects.requireNonNull(factory, "factory");
        PetTalent sample = factory.create();
        String id = sample.getId();
        if (!replace && factories.containsKey(id)) {
            throw new IllegalArgumentException("Talent with id " + id + " already registered");
        }
        factories.put(id, new TalentFactoryWrapper(sample, factory));
    }

    public void unregister(String id) {
        factories.remove(id);
    }

    public List<PetTalent> instantiate(List<String> ids) {
        List<PetTalent> talents = new ArrayList<>(ids.size());
        for (String id : ids) {
            TalentFactoryWrapper wrapper = factories.get(id);
            if (wrapper != null) {
                talents.add(wrapper.create());
            }
        }
        return talents;
    }

    public List<String> roll(Random random) {
        return roll(random, 2);
    }

    public List<String> roll(Random random, int count) {
        Objects.requireNonNull(random, "random");
        if (factories.isEmpty() || count <= 0) {
            return Collections.emptyList();
        }
        List<String> keys = new ArrayList<>(factories.keySet());
        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(keys.get(random.nextInt(keys.size())));
        }
        return result;
    }

    public List<String> inherit(List<String> parentA, List<String> parentB, Random random) {
        return inherit(parentA, parentB, random, 2);
    }

    public List<String> inherit(List<String> parentA, List<String> parentB, Random random, int count) {
        Objects.requireNonNull(parentA, "parentA");
        Objects.requireNonNull(parentB, "parentB");
        Objects.requireNonNull(random, "random");
        if (count <= 0) {
            return Collections.emptyList();
        }
        List<String> pool = new ArrayList<>();
        for (String id : parentA) {
            if (!pool.contains(id)) {
                pool.add(id);
            }
        }
        for (String id : parentB) {
            if (!pool.contains(id)) {
                pool.add(id);
            }
        }
        if (pool.isEmpty()) {
            return roll(random, count);
        }
        Collections.shuffle(pool, random);
        List<String> chosen = new ArrayList<>(pool.subList(0, Math.min(count, pool.size())));
        while (chosen.size() < count) {
            chosen.addAll(roll(random, 1));
        }
        return new ArrayList<>(chosen.subList(0, count));
    }

    @Override
    public Iterator<PetTalentDescriptor> iterator() {
        List<PetTalentDescriptor> descriptors = new ArrayList<>(factories.size());
        factories.values().forEach(wrapper -> descriptors.add(wrapper.descriptor));
        return descriptors.iterator();
    }

    @Override
    public PetTalentDescriptor get(String id) {
        TalentFactoryWrapper wrapper = factories.get(id);
        return wrapper != null ? wrapper.descriptor : null;
    }

    @Override
    public int size() {
        return factories.size();
    }

    private static final class TalentFactoryWrapper implements TalentFactory {
        private final PetTalentDescriptor descriptor;
        private final TalentFactory delegate;

        private TalentFactoryWrapper(PetTalent sample, TalentFactory delegate) {
            this.descriptor = new PetTalentDescriptor(sample.getId(), sample.getDisplayName(), sample.getDescription());
            this.delegate = delegate;
        }

        @Override
        public PetTalent create() {
            return delegate.create();
        }
    }
}
