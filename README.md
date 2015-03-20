scalajars.org
=============

This is the source code for the [Scalajars](http://scalajars/) jar
repository webapp.

It's basically a rip off of the awesome [Clojars](http://clojars.org) website.
Adapted for scala.
Because so far we were stuck with maven-central. It had to stop.

Running the webapp
------------------

There are several ways to run Scalajars depending on what you intend to do with
it. Regardless of how you run it, you first need to do some setup:

1. Install [Leiningen](http://github.com/technomancy/leiningen)
   * Mac OS X Homebrew: `brew install leiningen`

2. Install [SQLite3](http://www.sqlite.org/)
   * Debian: `apt-get install sqlite3`
   * Mac OS X Homebrew: `brew install sqlite`

3. Run the DB migrations: `lein migrate`

To run the application using Leinigen 2:

1. Run the webapp: `lein run` (see `--help` for options)

2. Now try hitting [localhost:8080](http://localhost:8080) in your web browser.

To build a standalone jar for deploying to a server:

1. Compile with: `lein uberjar`

2. Run the webapp: `java -jar target/scalajars-web-*-standalone.jar`

To run the application in auto-reload mode, from the console:

1. Run `lein ring server`

and that's it, it should automatically open a browser in [localhost:3000](http://localhost:3000).

If you'd like to run it out of an editor/IDE environment you can
probably eval a call to the `-main` function in
`src/scalajars/main.clj`.

Configuration
-------------

All options are available as command-line switches.  Additionally some
can be set using environment variables.  See `lein run -h` for the
full list.

Options may be read from a file using the `-f` switch, setting the
`CONFIG_FILE` environment variable or by putting a file named
`config.clj` on the classpath.  The config file should be a bare
Clojure map:

    {:db {:classname "org.sqlite.JDBC"
          :subprotocol "sqlite"
          :subname "data/dev_db"}
     :key-file "data/dev_authorized_keys"
     :repo "data/dev_repo"
     :bcrypt-work-factor 12
     :mail {:hostname "localhost"
            :from "sre@zengularity.com"
            :ssl false}}

The classpath option can be used with Leiningen 2 profiles.  When
running out of a source checkout using `lein run` the configuration
will be read from `dev-resources/config.clj`.  When running automated
tests with `lein test` then `test-resources/config.clj` is used.

SSH integration
---------------

The SSH integration is kind of a hack and needs improvement.
Currently it uses [Nailgun](http://martiansoftware.com/nailgun/) but
the current setup has threading problems due to the way it does IO.

Basically `clojars.scp` implements the [SCP protocol](http://blogs.sun.com/janp/entry/how_the_scp_protocol_works)
and acts as a Nailgun "nail" so it can be called from the
command-line.  Clojars writes an SSH `authorized_keys` file
with a line for each user's public key setting the login command to
`ng --nailgun-port 8700 clojars.scp USERNAME`.

To set it up:

1. Install the Nailgun `ng` C program.  Just unpack the Nailgun source
and run `make` then copy the `ng` executable somewhere like `/usr/local/bin`

2. Create a "clojars" unix account with a disabled password.

3. Disable password authentication for that user by adding this to
`/etc/ssh/sshd_config`:

        Match User clojars
        PasswordAuthentication no

4. Symlink in the authorized_keys file the webapp generates:

        cd /home/clojars
        mkdir .ssh
        cd .ssh
        ln -s ...../clojars-web/data/authorized_keys authorized_keys

5. When running the webapp enable the nailgun server: `--nailgun-port 8700`


License
-------

Copyright © 2009-2012 Alex Osborne, Phil Hagelberg, Nelson Morris and [contributors](https://github.com/ato/clojars-web/graphs/contributors).

Distributed under the Eclipse Public License, the same as Clojure. See the file COPYING.
