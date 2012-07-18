$(function(){
	
	if( $("#phrase_and_go").size() == 0 && $("#query_line").size() == 0 ){
		return; // ignore this js file if no query area
	}
	
    
    /***************************************************************************
	 * species entry behaviour
	 **************************************************************************/

	// autocomplete setup
	
    var species = new Array();
    $("#species_select > option").each(function(){
        species.push( $.trim($(this).text()) );
    });

    var alias = new Array();
    $("#species_select > option").each(function(i){
        var str = $(this).attr("alias");
    	alias.push( $.trim(str) );
    });
    
    var speciesAndAliases = new Array();
    for(var i in species) {
    	speciesAndAliases.push( species[i] );
    }
    for(var i in alias) {
    	speciesAndAliases.push( alias[i] );
    }

    $("#species_text").autocomplete(speciesAndAliases, {
        minChars: 2,
        matchContains: true,
        matchSubset: true,
        max: 5,
        selectFirst: true,
        autoFill: false,
        matchCase: false,
        focusFieldAfterCancel: false,
        focusFieldAfterSelect: false,
        delay: 10,
        onSetValue: updateSpecies,
        scroll: false
    }).bind("keydown", function(e){
    	if( e.which == 13 ){
    		return false; // do not submit form on enter
    	}
    });
    
    function validSpecies(spc) {
        for(var i in species) {
            if( spc == species[i] ) {
                return true;
            }
        }
        return false;
    }
    
    function validAlias(als) {
    	var ret = false;
    	if(als) {
	    	for(var i in alias) {
	            if( als == alias[i] ) {
	                ret = true;
	                break;
	            }
	        }
    	}
        return ret;
    }

    function getSpeciesFromAlias(als) {
    	var ret = null;
    	if(als) {
	    	for(var i in alias) {
	            if(alias[i] == als ) {
	                ret = species[i];
	                break;
	            }
	        }
    	}
        return ret;
    }
    
    function getIdForSpecies(spc){
    	for(var i in species){
    		if( species[i] == spc ){
    			return parseInt(i) + 1;
    		}
    	}
    	
    	return undefined;
    }
    
    function getIdForAlias(als){
    	for(var i in alias){
    		if( alias[i] == als ){
    			return parseInt(i) + 1;
    		}
    	}
    	
    	return undefined;
    }
    
    // clear on click of text box for convenience
    $("#species_text").focus(function() {
        $(this).val("");
    }).blur(function(){
    	updateSpecies();
    });
    
    function updateSpecies() {
        var val;
        
        try{
        	val = $("#species_text").val().trim();
        } catch(e){
        	val = null;
        }
        
//        console.log("--");
//        console.log(val);
        
        if( !val  || val == "" ) {
//        	console.log("grab from select on empty");
            resetSpeciesToSelectBoxVal();
        } else if( validSpecies(val) ) {
//        	console.log("set value to select on valid species");
            $("#species_select").val( getIdForSpecies(val) ).trigger("change");
        } else if( validAlias(val) ) {
//        	console.log("set value to select on valid alias");
            $("#species_text").val( getSpeciesFromAlias(val) );
			$("#species_select").val( getIdForAlias(val) ).trigger("change");
        } else {
//        	console.log("grab from select on invalid");
        	resetSpeciesToSelectBoxVal();
        }
        
    }
    
    $("#species_select").change(function(){
		var value = $(this).val();
		var option = $("#species_select option[value=" + value + "]");
		
		$("#posted_networks").remove();
		
		track("Species", "Change", option.text());
	});
    
    function resetSpeciesToSelectBoxVal() {
    	$("#species_text").val( $("#species_select option:selected").text() );
    }
    
    // on drop down button click, show menu
    $("#species_drop_down_closed").click(function() {
        // replace button
        $(this).hide();
        $("#species_drop_down_open").show();
        
        // replace widget
        $("#species_closed").css("visibility", "hidden");
        $("#species_open").show();
        
        // fancy ani
        $("#species_select").fadeIn(ANI_SPD, function(){
            $(this).focus();
            // $(this).scrollTop( $("#species_select
			// option:selected").offset().top );
        });
    });
    
    // on select blur, show the text box again
    $("#species_select").blur(function(){
        // replace button
        $("#species_drop_down_open").hide();
        $("#species_drop_down_closed").show();
    
        // replace widget
        $("#species_open").fadeOut(ANI_SPD);
        $("#species_closed").css("visibility", "visible");
        
        // fancy ani
        $(this).hide("slide", { direction: "up" }, ANI_SPD);
    });
    
    // close on select of menu
    $("#species_select").click(function(){
        $(this).blur();
    });
    
    // reset text value with value from select
	var prev_species = $("#species_select").val();
    $("#species_select").change(function() {
	    var current_species = $(this).val();
	    
	    if( current_species != prev_species ){
			$("#species_text").val( $("#species_select option:selected").text() );
			
			clearErrors();
			
			if( $("#advanced_options_open").is(":visible") ){
				loadNetworks();
			} 			
			
			invalidateGeneForOrganismChange();
			
			if( genesTouched() ){
				last_organism = $("#species_select").val();
				validateGeneSymbols();
			}
        }
        
	    prev_species = $("#species_select").val();
    });
    
//	if( $("#species_text").val() !="" ){
//    	$("#species_text").trigger("blur");
//    }
    
});
