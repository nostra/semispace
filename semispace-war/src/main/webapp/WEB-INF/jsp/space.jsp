<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html PUBLIC 
        "-//W3C//DTD XHTML 1.1 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
        
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
        <title>semispace-war</title>
</head>
<body>
    <h2>SemiSpace</h2>
    <div>
        You may want to expose one of the following interfaces 
        through apache proxy or something similar.
        <ul>
            <li> The unauthenticated space interface and <a href="services/space?wsdl">the wsdl definition</a></li>
            <li> The token authenticated space interface and <a href="services/tokenspace?wsdl">the wsdl definition</a></li>
        </ul>
        </div>
</body>
</html>