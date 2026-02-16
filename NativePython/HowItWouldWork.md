# The Vision: A Native Python Workflow for LogicMonitor Datasources

If LogicMonitor were to treat Python as a first-class citizen, the experience of building a Datasource would be as seamless as it is for Groovy or PowerShell, but with the added power of a modern, managed ecosystem.
Here is how a native Python implementation would look from a usability perspective:

## 1. Selection and Versioning

The workflow would begin just like any other Datasource. When a developer goes to the discovery or collection script section, they would select "Python" from the language dropdown.

Immediately, a new Runtime Version selector would appear. Instead of hoping the Collector has the right version installed, the author would explicitly choose from a list of prepackaged distributions provided by the Collector (e.g., Python 3.11 or 3.12). This ensures that the script runs in a predictable, isolated environment regardless of which Collector is executing it.

## 2. Integrated Dependency Management

One of the biggest hurdles today is managing third-party libraries. In this new UI, there would be a dedicated element for pip requirements.

* The author could simply list the packages they need (e.g., requests==2.31.0).
* The Collector would then automatically handle the creation of a managed virtual environment and the installation of those dependencies.
* This eliminates "environment drift" where scripts work on one Collector but fail on another because of a missing library.

## 3. Environment Variables & Property Integration

To replace the way Groovy uses hostProps and PowerShell uses tokens, a new Environment Variables UI element would be introduced. This would be the bridge between LogicMonitor's metadata and the Python script.

To make it truly "just work," host properties could be automatically injected into the environment. This could be handled in two ways:

* The JSON approach: One large LM_HOST_PROPERTIES variable containing a JSON string of all properties.
* The Individual approach: Mapping each property to a specifically named environment variable (e.g., PROP_SYSTEM_HOSTNAME).

This allows the author to use standard Python os.environ.get() calls, making the scripts cleaner and more portable.

## 4. The Script Block and Real-Time Testing

Finally, there would be the standard Script Block editor. Because the Collector is managing the runtime and dependencies, the "Test Script" functionality becomes significantly more reliable.

When you click "Test," the Collector can spin up the exact virtual environment defined in your requirements, inject the host properties as environment variables, and give you a deterministic result. There would be no more guessing if a failure is due to your code or a broken pip installation on the host OS.

## Why This is the Superior Path

By integrating these elements directly into the Datasource UI, LogicMonitor removes the "operational baggage" of Python. It transforms the Collector from a simple script executor into a managed extensibility platform.

This setup provides zero-setup development, deterministic portability, and secure execution—all while letting developers use the tools and libraries they already know and love.
