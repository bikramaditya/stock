<%@ page import="java.util.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="javax.naming.*" %>
<%@ page import="javax.sql.DataSource" %>

<html>
<head>
	<title>Index</title>
	<style>
	.center {
	    margin: auto;
	    width: 70%;
	    padding: 10px;
	    text-align: center;
	    vertical-align: middle;
	}
	.tableCenter {
		border:1px solid black;
	    margin: auto;
	    width: 68%;
	    padding: 0px;
	    text-align: center;
	    vertical-align: middle;
	}
	</style>
</head>
<body>
<div class="center">
	<h1>Welcome admin to stock predictor</h1>
	<h2>Please make your selection</h2>
	<table border="1" class="tableCenter">
		<tr><td>On Board New</td><td>Upload Holidays</td></tr>
		<tr><td><a href="Predict.jsp?MKT=NSE&predict=true">Predict and Trade</a></td><td><a href="/PredictorWeb/Validator.jsp">Validate</a></td></tr>
		<tr><td><a href="/PredictorWeb/Learn.jsp?MKT=NSE&learnAll=true">Learn all</a></td><td><a href="/PredictorWeb/Trader.jsp?learnAll=true">Place Orders</a></td></tr>
	</table>
</div>
</body>
</html>