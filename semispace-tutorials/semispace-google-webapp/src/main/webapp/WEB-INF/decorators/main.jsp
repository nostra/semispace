<!DOCTYPE html PUBLIC 
	"-//W3C//DTD XHTML 1.1 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="decorator" uri="http://www.opensymphony.com/sitemesh/decorator" %>
<%@taglib prefix="page" uri="http://www.opensymphony.com/sitemesh/page" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<title><decorator:title default="Spacepad"/></title>
    <link href="styles/tools.css" rel="stylesheet" type="text/css" media="all"/>
    <link href="styles/typo.css" rel="stylesheet" type="text/css" media="all"/>
    <link href="styles/forms.css" rel="stylesheet" type="text/css" media="all"/>
    <link href="styles/layout-navtop-localleft.css" rel="stylesheet" type="text/css" media="all"/>
    <link href="styles/layout.css" rel="stylesheet" type="text/css" media="all"/>
    <link href="styles/style.css" rel="stylesheet" type="text/css"/>
               
    <decorator:head/>
</head>
<body id="page-home">
    <div id="page">
		<div id="logo">
			<img src="images/header.png" alt="Logo"/> 
		</div>  
        <div id="content" class="clearfix">
            <div id="main">
            	<decorator:body/>
            </div>

			<div id="nav">
                <div class="wrapper">
                
                <ul class="clearfix">
                     <li><a href="index.html">index</a></li>
                     <c:if test="${empty googleKey}">
                     <li><a href="removeKey.html">remove key</a></li>
                     </c:if>
                     <li><a href="services/tokenspace?wsdl">WSDL</a></li>
                     <li class="last"><a href="services">services</a></li>
                </ul>
                </div>

            </div>
        </div>
        
      	<div id="copyright">
			<img src="images/footer.png" alt="footer"/> 
		</div>
            
    </div>
    
    <div id="extra1">&nbsp;</div>
    <div id="extra2">&nbsp;</div>
</body>
</html>
