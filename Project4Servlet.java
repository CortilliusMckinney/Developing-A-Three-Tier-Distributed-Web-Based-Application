/* Cortillius Mckinney
   CNT 4717- Spring 2018 - Project Four
   A Three-Tier Distributed Web-Based Application
   April 1, 2018
/*
// SurveyServlet.java
// A Web-based survey that uses JDBC from a servlet.

import java.sql.*;
import java.lang.*;
import java.io.PrintWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Project4Servlet extends HttpServlet
{
    
    // Data members to hold the connection that is established during init().
    private Connection connection;
    private Statement statement;
    private boolean connected;
    private String connectError;

    // set up database connection and create SQL statement
    public void init( ServletConfig config ) throws ServletException
    {
        // attempt database connection and create Statement
        connected = false;
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3310/project4", "root", "root" );
            connected = true;
            
            statement = connection.createStatement();
        } // end try
        catch ( Exception exception )
        {
            connectError = exception.toString();
            connected = false;
        } // end catch
    }  // end method init

    // process SQL
    protected void doPost( HttpServletRequest request,
                           HttpServletResponse response )
    throws ServletException, IOException
    {
        
        // set up response to client
        response.setContentType( "text/html" );
        PrintWriter out = response.getWriter();

        String sql = request.getParameter( "sql" ).trim();
        
        // Print the basic HTML form, up to "Database results:".
        printHeader(out, sql);

        // Only do the processing if the database is connected.
        if (connected) {

            // Allow default query if left blank.
            if (sql.equals("")) sql = "select * from suppliers";
            
            // Remove trailing semi-colon, if present.
            if (sql.charAt(sql.length()-1) == ';') {
                sql = sql.substring(0, sql.length()-1);
            }
            
            try
            {
                // execute the query. Don't know at this point if it is a SELECT
                // or an INSERT/UPDATE etc.
                if (statement.execute( sql )) {
                    // It's a SELECT. Get the metadata and set the result set.
                    ResultSet resultsRS = statement.getResultSet();
                    ResultSetMetaData metaData = resultsRS.getMetaData();
                    
                    // Prepare the table for display.
                    out.println("<table border=1>");
                    out.println("<tr>");
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        out.println("\t<th>" + metaData.getColumnName(i) + "</th>");
                    }
                    out.println("</tr>");


                    // Display each row.
                    while ( resultsRS.next() )
                    {
                        out.println("<tr>");
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            out.println("\t<td>" + resultsRS.getString(i) + "</td>");
                        }
                        out.println("</tr>");
                    } // end while
                    out.println("</table>");

                    // done.
                    resultsRS.close();

                } else {
                    
                    // It's not a SELECT. Do some basic output in Database results: section.
                    out.println("<p>Statement executed successfully.<br>");
                    out.println(statement.getUpdateCount() + " row(s) affected.</p>");
                    
                    // Do some basic parsing on the SQL. Split it into individual words, and
                    // then identify the index of the key sql tokens: WHERE, VALUES and SET.
                    // Also see if the "shipments" table is the target.
                    String[] words = sql.split("\\s");
                    int shipmentsIndex = -1;
                    int whereIndex = -1;
                    int valuesIndex = -1;
                    int setIndex = -1;
                    for (int i = 0; i < words.length; i++) {
                        if (words[i].equals("shipments") || words[i].equals("`shipments`") || words[i].equals("project4.shipments") || words[i].equals("`project4.shipments`")) {
                            shipmentsIndex = i;
                        }
                        if (words[i].equalsIgnoreCase("where")) {
                            whereIndex = i;
                        }
                        if (words[i].equalsIgnoreCase("values")) {
                            valuesIndex = i;
                        }
                        if (words[i].equalsIgnoreCase("set")) {
                            setIndex = i;
                        }

                    }
                    
                    // Determine if the business logic should be triggered.
                    boolean business = false;
                    boolean insertCase = false;
                    boolean updateCase = false;
                    if (shipmentsIndex >= 1) {
                        if (words[shipmentsIndex-1].equalsIgnoreCase("INSERT") || (shipmentsIndex >= 2 && words[shipmentsIndex-2].equalsIgnoreCase("INSERT"))) {
                            insertCase = true;
                            business = true;
                        }
                        if (words[shipmentsIndex-1].equalsIgnoreCase("UPDATE") || (shipmentsIndex >= 2 && words[shipmentsIndex-2].equalsIgnoreCase("UPDATE"))) {
                            updateCase = true;
                            business = true;
                        }
                    }
                    
                    // Only proceed if it's an INSERT or UPDATE on the shipments table.
                    if (business) {
                        
                        // Business Logic detected.
                        out.println("<p>Business Logic Detected! - Updating Supplier Status<p>");
                        int updateCount = 0;
                        
                        
                        // INSERT and UPDATE are handled differently.
                        if (insertCase) {
                            
                            // For an INSERT, we have to find the snum and the quantity to determine
                            // if the quantity qualifies for status update for the supplier.
                            
                            // We have to find the values that were inserted
                            String valuesClause = "";
                            for (int i = valuesIndex + 1; i < words.length; i++) {
                                valuesClause += words[i];
                            }
                            
                            // Remove parentheses around values clause, so we can use .split(",").
                            valuesClause = valuesClause.substring(1, valuesClause.length()-1);
                            
                            // Parse the values clause to get the snum and quantity.
                            String[] values = valuesClause.split(",");
                            String snum = values[0];
                            int quantity = Integer.parseInt(values[3]);
                            
                            // Update the supplier if quantity matches threshold.
                            if (quantity >= 100) {
                                sql = "UPDATE suppliers SET status=status+5 WHERE snum=" + snum;
                                updateCount = statement.executeUpdate(sql);
                            }
                            
                        } else {
                            // We need to know how much the quantity changed by, in order to know what the
                            // target quantity is to trigger the update in status. For example
                            // if the clauses is "SET quantity = quantity + 10", then we only trigger
                            // for shipments where the NEW quantity is between 100 and 109.
                            String setClause = "";
                            for (int i = setIndex+1; i < whereIndex && i < words.length; i++) {
                                setClause += words[i];
                            }
                            
                            // Now setClause looks like: "quantity=quantity+X" or "quantity=Y".
                            String equalsParts[] = setClause.split("=");
                            
                            // Only trigger business logic if it was the quantity that was changed.
                            if (equalsParts[0].equals("quantity")) {
                                int targetLow = 100;
                                int targetHigh = 100;
                                boolean supported = true;
                                
                                // Only trigger business logic if quantity increases. Only support "+"
                                // operator, because it gets too complicated if we support "*".
                                String plusParts[] = equalsParts[1].split("\\+");
                                if (plusParts.length == 1) {
                                    // "quantity=Y"
                                    try {
                                        int target = Integer.parseInt(plusParts[0]);
                                        targetLow = target;
                                        targetHigh = target;
                                    } catch (NumberFormatException ex) {
                                        // It was some other weird format, not supported.
                                        out.println("Business Logic for " + setClause + " is not supported.");
                                        supported = false;
                                    }
                                } else {
                                    // "quantity=quantity+X"
                                    try {
                                        targetHigh = 99 + Integer.parseInt(plusParts[1]);
                                    } catch (NumberFormatException ex) {
                                        // It was some other weird format, not supported.
                                        out.println("Business Logic for " + setClause + " is not supported.");
                                        supported = false;
                                    }
                                }

                                if (supported) {

                                    // We need to now examine the new quantity for each of the affected
                                    // rows in the shipments table. We select the affected rows using our
                                    // own SELECT, with the same WHERE clause the user used.
                                    String whereClause = "";
                                    for (int i = whereIndex; i > 0 && i < words.length; i++) {
                                        whereClause += " " + words[i];
                                    }
                                    sql = "SELECT snum, pnum, jnum, quantity FROM shipments";
                                    sql += whereClause;
                                    
                                    // Execute the query, and make a list of all the snum values that
                                    // were affected with quantities in the target range.
                                    ResultSet affectedRS = statement.executeQuery(sql);
                                    String inClause = "(";
                                    while (affectedRS.next()) {
                                        String snum = affectedRS.getString(1);
                                        int quantity = affectedRS.getInt(4);
                                        if (quantity >= targetLow && quantity <= targetHigh) {
                                            if (updateCount > 0) inClause += ",";
                                            inClause += "'" + snum + "'";
                                            updateCount++;
                                        }
                                    }
                                    affectedRS.close();
                                    inClause += ")";
                                    
                                    // Now update all the suppliers who were put in the list.
                                    if (updateCount > 0) {
                                        sql = "UPDATE suppliers SET status=status+5 WHERE snum in " + inClause;
                                        updateCount = statement.executeUpdate(sql);
                                    }
                                }
                            }
                        }
                        
                        // End of Business Logic. Note that updateCount could be zero, but it's still considered
                        // a trigger for the logic.
                        out.println("<p>Business Logic updated " + updateCount + " supplier status marks.");
                    }
                    
                }
            } // end try
            // if database exception occurs, return error page
            catch ( SQLException sqlException )
            {
                out.println( "<p><b>Error executing the SQL statement:</b><br>");
                out.println( sqlException.getMessage() + "</p>" );
            } // end catch
            
        } else {
            // Not connected, show error message.
                out.println( "<p>Database connection error.</p>" );
                out.println( "<pre>" + connectError + "</pre>");
        }

        // Finish the HTML page output.
        out.println( "</body></html>" );
        out.close();

    } // end method doPost

    // close SQL statements and database when servlet terminates
    public void destroy()
    {
        // attempt to close statements and database connection
        try
        {
            statement.close();
            connection.close();
        } // end try
        // handle database exceptions by returning error to client
        catch( SQLException sqlException )
        {
            sqlException.printStackTrace();
        } // end catch
    } // end method destroy
    
    
    // Outputs the top part of the web page, up to and including the form.
    private void printHeader(PrintWriter out, String sql) {
        
        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\">");
        out.println("<head>");
        out.println("   <title>CNT 4714 Remote Database Management System</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<center>");
        out.println("<h1>Welcome to the Project 4 Remote Database Management System</h1>");
        out.println("<hr>");
        out.println("<p>You are connected to the Project4 database.</p>");
        out.println("<p>Please enter any valid SQL query or update statement.</p>");
        out.println("<p>If no query/update command is given the Execute button will display all suplier information in the database.</p>");
        out.println("<p>All execution results will appear below.</p>");
		out.println("<textarea id=\"sqlTextArea\" name=\"sql\" form=\"queryForm\" rows=\"12\" cols=\"80\">" + sql + "</textarea>");
        out.println("<form id=\"queryForm\" action = \"/Project4/MySQLServlet\"  method = \"post\">");
        out.println("   <p>");
        out.println("   <p><input type = \"submit\" value = \"Execute\" /><input type=\"button\" value=\"Clear Form\" onclick='javascript:document.getElementById(\"sqlTextArea\").innerHTML=\"\";'/></p>");
        out.println("</form>");
        out.println("<hr>");
        out.println("Database Results:");

    }
    
    
} // end class SurveyServlet

