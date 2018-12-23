<%@ page import="java.util.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="javax.naming.*" %>
<%@ page import="javax.sql.DataSource" %>

<%@ page import="actions.*" %>
<%@ page import="entities.*" %>

<html>
<head>
	<title>Predict And Trade</title>
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
	<h1>Prediction cmplete</h1><br/><br/>	<br/>		
  <%
  	String mktName = request.getParameter("MKT");
  	String predict = request.getParameter("predict");
  	
  	if(predict!=null && predict.equals("true") && mktName!=null && mktName.length() > 0)
  	{
  		Learner learner = new Learner(mktName);
  		learner.predictAndTrade();
  	}
  	
  
  %>
  </table>
</div>  
</body>
</html>