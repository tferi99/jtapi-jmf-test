set LIB=libs_into_local_repo

rem call mvn install:install-file -DgroupId=com.sun -DartifactId=jmf -Dversion=2.1.1e -Dpackaging=jar -Dfile=%LIB%\jmf-2.1.1e.jar
call mvn install:install-file -DgroupId=com.cisco -DartifactId=jtapi -Dversion=11.5 -Dpackaging=jar -Dfile=%LIB%\jtapi-11.5.jar


pause

