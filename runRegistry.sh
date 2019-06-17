# %classpath cannot work in Windows
mvn exec:exec -Dexec.executable="java" -Dexec.args="-classpath %classpath org.ftoth.general.util.jmf.JMFRegistry"
