<!DOCTYPE html><html lang="en"><head>   <title>CNT 4714 Remote Database Management System</title></head><body><center><h1>Welcome to the Project 4 Remote Database Management System</h1><hr><p>You are connected to the Project4 database.</p><p>Please enter any valid SQL query or update statement.</p><p>If no query/update command is given the Execute button will display all suplier information in the database.</p><p>All execution results will appear below.</p>		<textarea id="sqlTextArea" name="sql" form="queryForm" rows="12" cols="80">select * from suppliers</textarea>   <form id="queryForm" action = "/Project4/MySQLServlet"  method = "post">   <p>   <p><input type = "submit" value = "Execute" /><input type="button" value="Clear Form" onclick='javascript:document.getElementById("sqlTextArea").innerHTML="";'/></p></form><hr>Database Results:</body></html>