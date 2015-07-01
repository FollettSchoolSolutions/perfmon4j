<%@page contentType="text/html" pageEncoding="UTF-8"%> 
<% 
	boolean hasError = request.getParameter("login_error") != null;
%>
<html> 
	<head> 
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title>Login to Perfmon4j Console</title>
		<style type="text/css">
		label {
			text-align: right;
			width: 100px;
			float: left;
		}
		</style>
	</head>	
	<body> 
		<h2>Login to Perfmon4j System Console</h2>
<% 
	if (hasError) {
%> 
		<h4 style="color: red;">Login Failed</h4>
<% 
	}
%>
		<form action="../j_spring_security_check" method="POST"> 
			<p><label>User:</label><input type="text" name="j_username"></p> 
			<p><label>Password:</label><input type="password" name="j_password"></p> 
			<p/>
			<input type="submit" value="Submit">
		</form> 
	</body> 
</html>
