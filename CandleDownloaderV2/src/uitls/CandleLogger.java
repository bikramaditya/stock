package uitls;
import java.util.Date;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import action.CandleDownloaderV2;
 
public class CandleLogger {
 
    PatternLayout patternLayoutObj = null;
    String conversionPattern = "[%p] %d %c %M - %m%n";
    
    // Create Daily Rolling Log File Appender
    DailyRollingFileAppender rollingAppenderObj = null;
    
    // Configure the Root Logger
    Logger rootLoggerObj = null;
    
    // Create a Customer Logger & Logs Messages
    Logger loggerObj = null;

    public CandleLogger()
    {
    	patternLayoutObj = new PatternLayout();
    	rollingAppenderObj = new DailyRollingFileAppender();
    	rollingAppenderObj.setFile("C:\\Stock\\Logs\\CandleDownloaderV2.log");
        rollingAppenderObj.setDatePattern("'.'yyyy-MM-dd");
        rollingAppenderObj.setLayout(patternLayoutObj);
        rollingAppenderObj.activateOptions();
        rootLoggerObj = Logger.getRootLogger();
        rootLoggerObj.setLevel(Level.DEBUG);
        rootLoggerObj.addAppender(rollingAppenderObj);
        loggerObj = Logger.getLogger(CandleDownloaderV2.class);
    }
    
    public void log(int severity, String message) {
    	message = new Date()+"-"+message;
        if(severity==1)
        {
        	loggerObj.error(message);
        }
        else
        {
        	loggerObj.info(message);           
        }
    }
}