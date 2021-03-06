package fr.landel.calc.processor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.landel.calc.config.I18n;
import fr.landel.calc.utils.DateUtils;
import fr.landel.calc.utils.Logger;
import fr.landel.calc.utils.MathUtils;
import fr.landel.calc.utils.StringUtils;

public class Entity {

    private static final Logger LOGGER = new Logger(Entity.class);

    public static final Map<String, Optional<Entity>> VARIABLES = new HashMap<>();

    public static final Pattern PATTERN_VARIABLE = Pattern.compile("^\\$[a-zA-Z_]+$");

    private static final Pattern PATTERN_NUMBER = Pattern.compile("([+-]?(:?[0-9]+(:?\\.[0-9]+)?|\\.[0-9]+)([Ee][+-]?[0-9]+)?)([a-zA-Z°'\"²³]+)?");
    private static final int GROUP_NUMBER = 0;
    private static final int GROUP_NUMBER_DECIMAL = 1;
    private static final int GROUP_NUMBER_UNITY = 5;

    private static final Pattern PATTERN_UNITY = Pattern.compile("^[a-zA-Z°'\"²³]+$");

    private final int index;
    private Double value;
    private Optional<LocalDateTime> date;
    private Optional<Duration> duration;
    private SortedSet<Unity> unities = new TreeSet<>(Unity.COMPARATOR_UNITIES);
    private boolean decimal;
    private boolean positive;
    private String variable;

    public Entity(final int index, final String input, final UnityType type) throws ProcessorException {
        this.index = index;
        parse(input, type);
        prepare();
    }

    public Entity(final int index, final String input) throws ProcessorException {
        this(index, input, null);
    }

    public Entity(final int index, final Double value, final Duration duration, final SortedSet<Unity> unities) {
        this.index = index;
        this.value = value;
        this.date = Optional.empty();
        this.duration = Optional.ofNullable(duration);
        this.setUnities(unities);
        prepare();
    }

    public Entity(final int index, final Double value, final Duration duration, final Unity... unities) {
        this.index = index;
        this.value = value;
        this.date = Optional.empty();
        this.duration = Optional.ofNullable(duration);
        this.setUnities(unities);
        prepare();
    }

    public Entity(final int index, final Double value, final LocalDateTime date, final SortedSet<Unity> unities) {
        this.index = index;
        this.value = value;
        this.date = Optional.ofNullable(date);
        this.duration = Optional.empty();
        this.setUnities(unities);
        prepare();
    }

    public Entity(final int index, final Double value, final LocalDateTime date, final Unity... unities) {
        this.index = index;
        this.value = value;
        this.date = Optional.ofNullable(date);
        this.duration = Optional.empty();
        this.setUnities(unities);
        prepare();
    }

    public Entity(final int index, final Double value, final SortedSet<Unity> unities) {
        this(index, value, (Duration) null, unities);
    }

    public Entity(final int index, final Double value, final Unity... unities) {
        this(index, value, (Duration) null, unities);
    }

    public Entity(final int index, final Double value) {
        this(index, value, Unity.NUMBER);
    }

    private void parse(final String input, final UnityType type) throws ProcessorException {
        final EntityTmp entity = new EntityTmp();
        final SortedMap<Unity, Double> inputs = new TreeMap<>(Unity.COMPARATOR_UNITIES);

        int notParsedlength = input.length();

        final Matcher matcher = PATTERN_NUMBER.matcher(input);
        while (matcher.find()) {
            if (entity.unity == null || entity.accumulable) {
                try {
                    notParsedlength -= matcher.group(GROUP_NUMBER).length();
                    entity.value = Double.parseDouble(matcher.group(GROUP_NUMBER_DECIMAL));

                    final String unityGroup = matcher.group(GROUP_NUMBER_UNITY);
                    entity.unities = Unity.getUnities(unityGroup, type);

                    if (entity.unities.isEmpty() && !entity.accumulable) {
                        this.value = entity.value;

                    } else if (entity.unities.size() > 1) {
                        throw new ProcessorException(I18n.ERROR_UNITY_UNKNOWN, unityGroup);

                    } else {
                        this.check(entity, input, inputs);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error(e, I18n.ERROR_FORMULA_PARSE.getI18n(), input);
                    throw new ProcessorException(e, I18n.ERROR_FORMULA_PARSE, input);
                }
            } else {
                throw new ProcessorException(I18n.ERROR_FORMULA_FORMAT, input);
            }
        }

        if (UnityType.DATE.equals(entity.unityType)) {
            loadDuration(inputs);
            loadLocalDateTime(inputs);

        } else if (input.startsWith(StringUtils.DOLLAR)) {
            loadVariables(input);
            notParsedlength = 0;

        } else if (!this.isNumber() && !this.hasUnity()) {
            loadUnities(input, type);
            notParsedlength = 0;
        }

        if (notParsedlength > 0) {
            throw new ProcessorException(I18n.ERROR_FORMULA_PARSE, input);
        }

        if (this.date == null) {
            this.date = Optional.empty();
        }
        if (this.duration == null) {
            this.duration = Optional.empty();
        }
    }

    private void check(final EntityTmp entity, final String input, final SortedMap<Unity, Double> inputs) throws ProcessorException {

        if (entity.unities.isEmpty()) {
            entity.unity = entity.unity.next().orElseThrow(() -> new ProcessorException(I18n.ERROR_FORMULA_FORMAT, input));
        } else {
            entity.unity = entity.unities.first();
        }

        entity.unityType = entity.unity.getType();
        entity.accumulable = entity.unityType.isAccumulable();

        if (this.hasUnity() && !Objects.equals(this.getUnityType(), entity.unityType)) {
            throw new ProcessorException(I18n.ERROR_FORMULA_FORMAT, input);

        } else if (inputs.containsKey(entity.unity) || (Unity.INCOMPATIBLE_UNITIES.containsKey(entity.unity)
                && Unity.INCOMPATIBLE_UNITIES.get(entity.unity).stream().anyMatch(inputs::containsKey))) {
            throw new ProcessorException(I18n.ERROR_FORMULA_UNITIES, input);

        } else {
            inputs.put(entity.unity, entity.value);
        }

        if (!UnityType.DATE.equals(entity.unityType)) {
            this.getUnities().add(entity.unity);
            entity.value = entity.unity.fromUnity(entity.value);

            if (this.value == null) {
                this.value = entity.value;
            } else {
                this.value += entity.value;
            }
        }
    }

    private void loadVariables(final String input) throws ProcessorException {
        if (PATTERN_VARIABLE.matcher(input).matches()) {

            this.variable = input;

            final Optional<Entity> entity = VARIABLES.get(this.variable);

            if (entity == null || entity.isEmpty()) {
                this.unities.add(Unity.VARIABLE);
                VARIABLES.put(input, Optional.empty());

            } else {
                loadEntity(entity.get());
            }
        }
        if (!this.isVariable()) {
            LOGGER.error(I18n.ERROR_FORMULA_PARSE.getI18n(), input);
            throw new ProcessorException(I18n.ERROR_FORMULA_PARSE, input);
        }
    }

    private void loadUnities(final String input, final UnityType type) throws ProcessorException {
        if (PATTERN_UNITY.matcher(input).matches()) {
            final SortedSet<Unity> list = Unity.getUnities(input, type);
            if (!list.isEmpty() && !this.hasUnity()) {
                this.setUnities(list.toArray(Unity[]::new));
            }
        }
        if (!this.hasUnity()) {
            LOGGER.error(I18n.ERROR_FORMULA_PARSE.getI18n(), input);
            throw new ProcessorException(I18n.ERROR_FORMULA_PARSE, input);
        }
    }

    private void loadDuration(final SortedMap<Unity, Double> inputs) throws ProcessorException {
        if (inputs.containsKey(Unity.DATE_YEAR)) {
            return;
        }

        final Duration duration = Unity.mapToDuration(inputs, this.getUnities());

        this.value = Double.valueOf(duration.toNanos());
        this.duration = Optional.of(duration);
    }

    private void loadLocalDateTime(final SortedMap<Unity, Double> inputs) throws ProcessorException {
        if (!inputs.containsKey(Unity.DATE_YEAR)) {
            return;
        }

        final LocalDateTime date = Unity.mapToLocalDateTime(inputs, this.getUnities());

        this.value = DateUtils.toZeroNanosecond(date);
        this.date = Optional.of(date);
    }

    private void prepare() {
        if (this.isNumber()) {
            positive = MathUtils.isEqualOrGreater(this.getValue(), 0d, MainProcessor.getPrecision());
            decimal = MathUtils.isNotEqual(this.getValue(), Math.round(this.getValue()), MainProcessor.getPrecision());
        }
    }

    public Unity firstUnity() {
        return this.unities.first();
    }

    public boolean hasUnity() {
        return !this.unities.isEmpty();
    }

    public Double getValue() {
        return this.value;
    }

    public Optional<LocalDateTime> getDate() {
        return this.date;
    }

    public Optional<Duration> getDuration() {
        return this.duration;
    }

    public boolean isNumber() {
        return this.value != null && (!this.hasUnity() || Unity.NUMBER.equals(this.firstUnity()));
    }

    public boolean isVariable() {
        return this.variable != null;
    }

    public boolean isUnity() {
        return this.value == null;
    }

    public boolean isPositive() {
        return this.positive;
    }

    public boolean isDate() {
        return this.date.isPresent();
    }

    public boolean isDuration() {
        return this.duration.isPresent();
    }

    public SortedSet<Unity> getUnities() {
        return this.unities;
    }

    public Entity setUnities(final SortedSet<Unity> unities) {
        this.unities = unities;
        return this;
    }

    public Entity setUnities(final Unity... unities) {
        this.unities = new TreeSet<>(Unity.COMPARATOR_UNITIES);
        this.unities.addAll(Arrays.asList(unities));
        return this;
    }

    public UnityType getUnityType() {
        if (this.hasUnity()) {
            return this.firstUnity().getType();
        } else {
            return UnityType.NUMBER;
        }
    }

    public Double toUnityOrValue() {
        if (this.hasUnity()) {
            return this.firstUnity().toUnity(this.value);
        } else {
            return this.value;
        }
    }

    public Double toUnity() {
        if (this.hasUnity()) {
            return this.firstUnity().toUnity(this.value);
        } else {
            return this.value;
        }
    }

    public Double toUnity(final Double value) {
        if (this.hasUnity()) {
            return this.firstUnity().toUnity(value);
        } else {
            return value;
        }
    }

    public Double fromUnity(final Double value) {
        if (this.hasUnity()) {
            return this.firstUnity().fromUnity(value);
        } else {
            return value;
        }
    }

    public int getIndex() {
        return this.index;
    }

    public boolean isInteger() {
        return !this.decimal;
    }

    public boolean isPositiveDecimal() {
        return this.positive;
    }

    public boolean isDecimal() {
        return this.decimal;
    }

    public boolean isUnity(final UnityType type) {
        return this.isUnity() && type.equals(this.getUnityType());
    }

    public String getVariable() {
        return this.variable;
    }

    public Entity setVariable(final Entity entity) {
        loadEntity(entity);
        VARIABLES.put(this.variable, Optional.of(this));

        return this;
    }

    private void loadEntity(final Entity entity) {
        this.value = entity.getValue();
        this.date = entity.getDate();
        this.duration = entity.getDuration();
        this.decimal = entity.isDecimal();
        this.positive = entity.isPositive();
        this.unities = entity.getUnities();
    }

    @Override
    public String toString() {
        return this.getUnityType().format(this);
    }

    private class EntityTmp {
        Double value;
        Unity unity;
        UnityType unityType;
        boolean accumulable;
        SortedSet<Unity> unities;
    }
}