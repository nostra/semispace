<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html PUBLIC 
        "-//W3C//DTD XHTML 1.1 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
        
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
        <title>Google-webapp</title>
</head>
<body>
    <c:if test="${! empty googleKey}">
    <h2>Enter google map key</h2>
    <p>You need to add a key before you can query google. 
    <a href="http://code.google.com/apis/maps/">Obtain a key from google maps</a>.</p>
    <form:form action="submitGoogleKey.html" commandName="googleKey" >
    <form:input path="key" size="78"/>
    <input type="submit" value="Submit google key" /></td>
    </form:form>    
    </c:if>
    
    <c:if test="${! empty searchForAddress}">
    <h2>Search for address</h2>
    <c:if test="${! empty googleKey}">
    <p>Notice that you do not get a result from google, as you have not
    submitted a google key yet. However, by watching the logs, you will
    see that things happen.</p>
    </c:if>
    <div>
    <form:form action="submitAddress.html" commandName="searchForAddress" >
    Enter address: <form:input path="address" size="78"/>
    <input type="submit" value="Submit" />
    </form:form>    
    </div>
    </c:if>

    <c:if test="${! empty addressResult}">
    <h2>Query result</h2>
    <pre>
    <c:out value="${addressResult}" escapeXml="false" />
    </pre>
    </c:if>
        
    <c:if test="${! empty user}">
    <h2>Enter new user</h2>
    <p>Enter user name and password. If the password is empty, the user is removed.</p>
    <div>
    <form:form action="submitUser.html" commandName="user" >
    User name: <form:input path="username" size="78"/>
    Password: <form:input path="password" size="78"/>
    <input type="submit" value="New or updated user info" />
    </form:form>    
    </div>
    </c:if>
    
    <c:if test="${! empty userList}">
    <h2>User list</h2>
    <c:if test="${empty userList.users}">
    <p>You need to add some users</p>
    </c:if>
    <ul>
    <c:forEach items="${userList.users}" var="usr">
    <li><c:out value="${usr}" escapeXml="true" /></li>
    </c:forEach>
    </ul>
    </c:if>
</body>
</html>