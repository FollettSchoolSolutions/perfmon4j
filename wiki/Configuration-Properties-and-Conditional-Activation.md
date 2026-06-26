# Configuration Properties & Conditional Activation

Perfmon4j's `perfmonconfig.xml` file supports **property substitution** and **conditional activation**. Together these let a single configuration file adapt to its environment — for example, enabling a monitor only when a particular environment variable is set, or pulling an appender's connection details from system properties.

This page documents the syntax, the property resolution order, and how to conditionally enable parts of your configuration. Every behavior described here is backed by a unit test in `base/src/test/java/org/perfmon4j/XMLConfigurationParserTest.java`.

---

## Table of Contents

- [Property Substitution Syntax](#property-substitution-syntax)
- [Where Substitution Works](#where-substitution-works)
- [Property Resolution Order](#property-resolution-order)
  - [Environment Variables](#environment-variables)
  - [Forcing an Environment-Variable Lookup](#forcing-an-environment-variable-lookup)
- [Defining Local Properties](#defining-local-properties)
  - [Referencing Properties in Later Definitions](#referencing-properties-in-later-definitions)
- [Conditional Activation](#conditional-activation)
  - [Activate If a Property Exists](#activate-if-a-property-exists)
  - [Activate If a Property Matches a Value](#activate-if-a-property-matches-a-value)
  - [Requiring Multiple Conditions](#requiring-multiple-conditions)
  - [Activation Rules & Limitations](#activation-rules--limitations)
- [The `enabled` Attribute](#the-enabled-attribute)
- [Recipe: Enable a Monitor From an Environment Variable](#recipe-enable-a-monitor-from-an-environment-variable)
- [Worked Example: Conditional InfluxDB Appender](#worked-example-conditional-influxdb-appender)
- [Quick Reference](#quick-reference)

---

## Property Substitution Syntax

Property references use the form `${...}`:

| Form | Meaning |
|------|---------|
| `${name}` | Substitute the resolved value of `name`. If unresolved, the token is left unchanged. |
| `${name:defaultValue}` | Substitute the resolved value of `name`, or `defaultValue` if `name` is not resolvable. |

Substitution is recursive — a resolved value that itself contains a `${...}` token is expanded as well.

```xml
<appender name='text-out'
          className='org.perfmon4j.TextAppender'
          interval='${MONITOR_INTERVAL:1 minute}'/>
```

If `MONITOR_INTERVAL` is defined, its value is used as the interval; otherwise the appender falls back to `1 minute`.

---

## Where Substitution Works

Substitution is applied to **both attribute values and element body text** throughout the config file.

Attribute value (interval resolved from a property, with a default):

```xml
<appender name='5 minute'
          className='...'
          interval='${textAppender.defaultInterval:5 min}'/>
```

Attribute value (monitor name resolved from a system property):

```xml
<monitor name='${monitorName}'>
    <appender name='5 minute'/>
</monitor>
```

Element body (resolved from an environment variable):

```xml
<property name='someKey'>${SOME_ENV_VARIABLE}</property>
```

---

## Property Resolution Order

When a `${name}` token is resolved, Perfmon4j checks sources in the following order and uses the **first** match:

1. **Local properties** — values declared in `<properties>` blocks within the config file.
2. **JVM system properties** — supplied via `-Dname=value` on the command line.
3. **Operating-system environment variables**.

So a system property takes precedence over an environment variable of the same name, and a locally-defined property takes precedence over both.

### Environment Variables

By default, environment variables are resolved automatically — no special prefix is required. A token such as `${JAVA_HOME}` will resolve to the `JAVA_HOME` environment variable if it is not first satisfied by a local or system property.

### Forcing an Environment-Variable Lookup

Prefix the name with `env.` to bypass local/system properties and read the environment variable directly. This is useful when a system property and an environment variable share the same name but you specifically want the environment value:

| Token | Resolves to |
|-------|-------------|
| `${USERNAME}` | System property `USERNAME` if set, otherwise the `USERNAME` environment variable. |
| `${env.USERNAME}` | The `USERNAME` **environment variable**, regardless of any system property. |

---

## Defining Local Properties

Declare local properties inside one or more `<properties>` blocks. Each `<property>` element has a `name` attribute and a body value:

```xml
<Perfmon4JConfig>
    <properties>
        <property name='prop1'>test1</property>
        <property name='prop2'>test2</property>
    </properties>
</Perfmon4JConfig>
```

Multiple `<properties>` blocks are allowed and are processed in document order:

```xml
<Perfmon4JConfig>
    <properties>
        <property name='prop1'>test1</property>
    </properties>
    <properties>
        <property name='prop2'>test2</property>
    </properties>
</Perfmon4JConfig>
```

### Referencing Properties in Later Definitions

A property defined earlier in the file can be referenced by `${...}` in **both the name and the value** of a property defined later (including in a later `<properties>` block):

```xml
<properties>
    <property name='prop1'>test1</property>
</properties>
<properties>
    <!-- Both the name and the value below expand 'prop1' -->
    <property name='prop2-${prop1}'>test2-${prop1}</property>
</properties>
```

After parsing, this defines a property named `prop2-test1` with the value `test2-test1`.

---

## Conditional Activation

A `<properties>` block may contain a single `<activation>` element. The properties in that block are only applied if the activation condition is satisfied. Combined with the `enabled` attribute (below), this is the primary mechanism for conditionally turning configuration on and off.

### Activate If a Property Exists

An activation `<property>` with **no body** matches if the named property simply **exists** (any value, including an empty string):

```xml
<properties>
    <activation>
        <!-- Apply this block only if 'verbosePerfmonOutput' is defined -->
        <property name='verbosePerfmonOutput'/>
    </activation>
    <property name='prop1'>activated</property>
</properties>
```

- If `verbosePerfmonOutput` is **not** defined, `prop1` is never set.
- If `verbosePerfmonOutput` is defined (even as an empty string), `prop1` is set to `activated`.

### Activate If a Property Matches a Value

An activation `<property>` with a body matches only if the named property resolves to **exactly** that value:

```xml
<properties>
    <activation>
        <!-- Apply only if 'perfmonOutputLevel' equals 'verbose' -->
        <property name='perfmonOutputLevel'>verbose</property>
    </activation>
    <property name='prop1'>verbose</property>
</properties>
```

- `perfmonOutputLevel=default` → block is **not** applied.
- `perfmonOutputLevel=verbose` → `prop1` is set.

### Requiring Multiple Conditions

A single `<activation>` element may list several `<property>` conditions. **All** of them must match for the block to be applied:

```xml
<properties>
    <activation>
        <property name='influxGroups'/>
        <property name='influxPassword'/>
    </activation>
    <property name='influxEnabled'>true</property>
</properties>
```

Here `influxEnabled` is only set to `true` when **both** `influxGroups` and `influxPassword` are defined. If only one is present, the block is skipped.

### Activation Rules & Limitations

| Rule | Behavior |
|------|----------|
| One `<activation>` per `<properties>` block | A `<properties>` block containing **more than one** `<activation>` element is invalid. Perfmon4j rejects the configuration and falls back to the default configuration. |
| Multiple conditions within one `<activation>` | Allowed — treated as a logical **AND** (all must match). |
| Empty-body condition | Matches if the property **exists** (any value). |
| Valued condition | Matches only on an **exact** string equality. |

---

## The `enabled` Attribute

Most top-level configuration elements accept an `enabled` attribute. When `enabled` resolves to anything other than the literal string `true` (case-insensitive), the element is treated as disabled and excluded from the running configuration. A missing or blank `enabled` attribute defaults to **enabled**.

`enabled` is honored on:

- `<Perfmon4JConfig>` (the root element)
- `<appender>`
- `<monitor>`
- `<snapShotMonitor>`
- `<emitterMonitor>`
- `<threadTrace>`

```xml
<monitor enabled='false' name='myMon'>
    <appender name='myAppender'/>
</monitor>
```

A disabled element is dropped during parsing. Note the **cascade behavior**: if an `<appender>` is disabled, any monitor, snapshot, emitter, or thread trace whose **only** appender was that disabled appender is also dropped, since it has nothing left to write to.

Because `enabled` is an ordinary attribute, it is subject to property substitution — which is what makes environment-driven enabling possible.

---

## Recipe: Enable a Monitor From an Environment Variable

To switch a monitor (or appender, snapshot, etc.) on or off based on an environment variable, point its `enabled` attribute at a `${...}` token and supply a safe default.

```xml
<snapShotMonitor name='JVMSnapShot'
                 className='org.perfmon4j.java.management.JVMSnapShot'
                 enabled='${PERFMON4J_ENABLE_JVM:false}'>
    <appender name='text-out'/>
</snapShotMonitor>
```

Behavior:

| Environment | Result |
|-------------|--------|
| `PERFMON4J_ENABLE_JVM=true` | Resolves to `true` → monitor **enabled**. |
| `PERFMON4J_ENABLE_JVM` unset | Falls back to the default `false` → monitor **disabled**. |
| `PERFMON4J_ENABLE_JVM=0` (or `no`, `yes`, etc.) | Any value other than `true` → monitor **disabled**. |

> **Tip:** Always provide a `:default` so an unset variable has well-defined behavior. For an opt-in monitor, default to `false` (off unless explicitly enabled).
>
> **Tip:** The `enabled` comparison is strict equality against `true`, not general truthiness. Keep your environment values to the literal strings `true` / `false`.

---

## Worked Example: Conditional InfluxDB Appender

This pattern combines activation and the `enabled` attribute so an InfluxDB appender is only configured when **both** the required credentials are present in the environment:

```xml
<Perfmon4JConfig>

    <!-- Derive a single 'influxEnabled' flag, true only when BOTH
         influxGroups and influxPassword are defined in the environment. -->
    <properties>
        <activation>
            <property name='influxGroups'/>
            <property name='influxPassword'/>
        </activation>
        <property name='influxEnabled'>true</property>
    </properties>

    <!-- The appender enables itself from the derived flag, defaulting to false. -->
    <appender name='influx'
              className='org.perfmon4j.influxdb.InfluxAppender'
              interval='1 minute'
              enabled='${influxEnabled:false}'>
        <attribute name='groups'>${influxGroups}</attribute>
        <attribute name='password'>${influxPassword}</attribute>
    </appender>

    <monitor name='WebRequest'>
        <appender name='influx' pattern='.'/>
    </monitor>

</Perfmon4JConfig>
```

- Neither variable set → `influxEnabled` is never defined → `enabled` falls back to `false` → the appender is dropped (and the `WebRequest` monitor, having no other appender, is dropped with it).
- Only one variable set → activation fails → same result.
- Both set → `influxEnabled` becomes `true` → the appender is enabled and reads its credentials from the same environment values.

---

## Quick Reference

| Syntax | Purpose |
|--------|---------|
| `${name}` | Substitute property/system-property/env-var `name`. |
| `${name:default}` | Substitute `name`, or `default` if unresolved. |
| `${env.name}` | Force resolution from the environment variable `name`. |
| `<properties>` | Container for local property definitions. |
| `<property name='x'>value</property>` | Define local property `x`. |
| `<activation>` | Gate the enclosing `<properties>` block on one or more conditions (one `<activation>` per block). |
| `<property name='x'/>` (in activation) | Condition: matches if `x` **exists**. |
| `<property name='x'>v</property>` (in activation) | Condition: matches if `x` **equals** `v`. |
| `enabled='${VAR:false}'` | Enable an element only when `VAR` resolves to `true`. |

**Resolution order:** local properties → JVM system properties (`-D`) → OS environment variables.

> See also: [Configuring the Java Agent](Configuring-the-Java-Agent.md) for command-line agent parameters and the overall structure of `perfmonconfig.xml`.
