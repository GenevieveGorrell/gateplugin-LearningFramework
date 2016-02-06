/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.engines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

/**
 * Some utilities to make it easy to extract the values of parameters as needed.
 * Parameters are specified as a string for training, application, evaluation and 
 * exporting. Which parameters can be used and what they do depends entirely on the 
 * situation and the algorithm used. Each algorithm will just look if any of the parameters
 * it knows is there and use those which are there - all other parameters are simply ignore.
 * If a parameter is there and cannot be converted to the required type, an exception is thrown
 * by the engine using the parameter.
 * 
 * The syntax of parameters is identical to how long options are specified on the command line,
 * e.g. "-maxDepth 3 -prune" sets the value of parameter "maxDepth" to "2" and the value of 
 * parameter "prune" to "true" (all Strings which will subsequently get converted to Integer and 
 * Boolean, respectively)
 * 
 * IMPORTANT NOTE: at the moment we use a slightly modified version of commons.cli which 
 * allows us to ignore unknown options by calling ignoreUnknownOptions(true) (this method has been
 * added)
 * @author Johann Petrak
 */
public class Parms {
  private static Logger logger = Logger.getLogger(Parms.class.getName());
  /**
   * Parse the given string and return a map that contains the value for the names given.
   * The names are strings which consist of three parts, separated by colons: a short name,
   * a long name, and either 0, if the parameter is boolean or 1 if a value is expected.
   * @param names
   * @param parmString
   * @return 
   */
  public static Map<String,String> getParameters(String parmString, String... names) {
    Map<String,String> ret = new HashMap<String,String>();
    List<String> longNames = new ArrayList<String>();
    Options options = new Options();
    for(String name : names) {
      String[] info = name.split(":");
      longNames.add(info[1]);
      options.addOption(info[0], info[1], info[2].equals("0") ? false : true, "");
    }
    DefaultParser parser = new DefaultParser();
    parser.setIgnoreUnknownOptions(true);
    CommandLine cli = null;
    try {
      cli = parser.parse(options,parmString.split("\\s+"));
    } catch (ParseException ex) {
      //System.err.println("Parsing error");
      //ex.printStackTrace(System.err);
      logger.error("Could not parse parameters: "+parmString,ex);
    }
    if(cli != null) {
      for(String longName : longNames) {
        ret.put(longName, cli.getOptionValue(longName));
      }
    }
    return ret;
  }
  
  /*
  private static class MyParser extends DefaultParser {
    @Override
    protected void handleUnknownToken(String token) throws ParseException {
      cmd.addArg(token);
    }
  }
  */
  
}
