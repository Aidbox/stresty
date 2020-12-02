.EXPORT_ALL_VARIABLES:

clean:
	rm pom.xml && rm -rf .cpcache

repl:
	clj -A:nrepl:ui -m nrepl.cmdline --middleware "[cider.nrepl/cider-middleware refactor-nrepl.middleware/wrap-refactor]"

resources/VERSION:
	cp VERSION resources/VERSION

# export GRAALVM_HOME=$HOME/graalvm/Contents/Home
# clojure -A:native-image --graalvm-opt 'H:ReflectionConfigurationFiles=reflection.json'
build-native: resources/VERSION
	clojure -A:native-image

build: resources/VERSION
	clojure -A:run-test && clojure -A:build --app-version `cat VERSION` && cp target/stresty-*-standalone.jar target/stresty.jar
	rm resources/VERSION

npm:
	npm install
