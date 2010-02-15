<!DOCTYPE html PUBLIC 
	"-//W3C//DTD XHTML 1.1 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="decorator" uri="http://www.opensymphony.com/sitemesh/decorator" %>
<%@taglib prefix="page" uri="http://www.opensymphony.com/sitemesh/page" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<title><decorator:title default="Babel"/></title>
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

                <%-- 
			<div id="nav">
                <div class="wrapper">
                <ul class="clearfix">
                     <li><a href="spacepad.html">spacepad</a></li>
                     <li><a href="menu2.html">menu item 2</a></li>

                    <li class="last">
                    <a href="menu3.html">Last menu item</a>
                     </li>
                </ul>
                </div>
            </div>
            --%>
        </div>
        
      	<div id="copyright">
			<img src="images/footer.png" alt="footer"/> 
		</div>
            
    </div>
    
    <div id="extra1">&nbsp;</div>
    <div id="extra2">&nbsp;</div>
</body>
</html>
