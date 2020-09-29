    // hide all nodes below this parent
    function hide(parent) {
	var plus = document.getElementById(parent + "_plus");
	var minus = document.getElementById(parent + "_minus");
	var desc = document.getElementById(parent + "_desc");

	if(plus == null) {
	    updateTableList(parent, false);	
	    return;
	}
				
	plus.style.display="";
	minus.style.display = "none";
	desc.style.display = "none";
	updateTableList(parent, false);			
    }

    // show all nodes below this parent
    function show(parent) {
	var plus = document.getElementById(parent + "_plus");
	var minus = document.getElementById(parent + "_minus");
	var desc = document.getElementById(parent + "_desc");
	
	if(minus == null) {
	    updateTableList(parent, false);	
	    return;
	}
				
	plus.style.display="none";
	minus.style.display = "inline";
	desc.style.display = "block";
	updateTableList(parent, true);
    }
			

    // hide all nodes
    function hideAll(nodes) {

	for(i = 1; i <= nodes; i++)
		hide(i);
    }


    // show all nodes
    function showAll(nodes) {

	for(i = 1; i <= nodes; i++)
		show(i);
    }


			
    // Create a cookie with the specified name and value.
    function setCookie(sName, sValue) {
	date = new Date();
	
	// Expire in a year
	date.setTime(date.getTime()+(365*24*60*60*1000));
	var expireTime = date.toGMTString();
	document.cookie = sName + "=" + escape(sValue) + "; expires=" + expireTime;
    }
			
    // Get value of cookie of given name
    function getCookie(sName) {
			 		 
	// cookies are separated by semicolons
	var aCookie = document.cookie.split("; ");
	for (var i=0; i < aCookie.length; i++) {
	    // a name/value pair (a crumb) is separated by an equal sign
	    var aCrumb = aCookie[i].split("=");
	    if (sName == aCrumb[0]) 
		return unescape(aCrumb[1]);
	}
    
	// a cookie with the requested name does not exist
	return null;
    }			
			
    // Update list of entries to expand when page is loaded		
    function updateTableList(parent, opening) {
	var openList = getCookie('tableState');
				
	// If we don't have cookie yet 
	if(openList == null && opening) {
	    setCookie('tableState', parent + ",");
	    return;
	}

	var entries = openList.split(",");
				

	//We need to add entry to cookie, but ensure no duplicates
	var newEntry = '';
					
	if(opening) {
	    newEntry = parent + ',';
	}
	
			    
	for(var j = 0; j < entries.length; j++) { 
	    // We already have entry for parent
	    if(entries[j] == parent) {
		continue;
	    }
						   
						  
	    // Remove null entries
	    if(entries[j] == '') {
		continue;
	    }
						   
	    newEntry += entries[j] + ",";
	}
        
	// remove trailing ","                        
	newEntry = newEntry.substr(0, newEntry.lastIndexOf(','));

	// remove leading ","
	if(newEntry.charAt(0) == ',')
	    newEntry = newEntry.substr(1);

	// update cookie
	setCookie('tableState', newEntry);
    }
			

    // called when page is loaded to return to previous table state
    function restoreTableState() {
			   
	var openList = getCookie('tableState');
			   
	// If no cookie, nothing to do
	if(openList == null) {
	    return;
	}
				

	// Expand entries
	var entries = openList.split(",");
	for(var j = 0; j < entries.length; j++) {
	    show(entries[j]);
	}
    }
		