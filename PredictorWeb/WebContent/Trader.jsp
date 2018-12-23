<%@ page import="java.util.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="javax.naming.*" %>
<%@ page import="javax.sql.DataSource" %>

<%@ page import="actions.*" %>
<%@ page import="entities.*" %>

<html>
<head>
	<title>Trader</title>
	<style>
	.center {
	    margin: auto;
	    width: 80%;
	    padding: 10px;
	    text-align: center;
	    vertical-align: middle;
	    font-weight: normal;
	}
	.tableCenter {
		border:1px solid black;
	    margin: auto;
	    width: 90%;
	    padding: 0px;
	    text-align: center;
	    vertical-align: middle;
	    font-size:14px;
	    font-weight: normal;
	}
	</style>
</head>
<body>
<h3><a href="/PredictorWeb">Back to Home</a></h3><br/>
<div class="center">
	<h1>Welcome and help the system to learn a stock</h1><br/><br/>	<br/>		
  <%
    String stockName = request.getParameter("SYMBOL");
  	String mktName = request.getParameter("MKT");
  	String learnAll = request.getParameter("learnAll");
  	
  	Trader trader = new Trader();
  	trader.placeTrades();
  	
  %>
  </table>
</div>  
</body>
</html>