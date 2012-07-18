function loadDefaultOrganismNetworks() {
	// console.log("load networks");
		
	startNetworksLoader();

	var currentSpeciesIndex = $("#species_select").attr("selectedIndex");
}

var _networks_reloading_count = 0;

function adjustNetworksLoaderCSS(){
	if( !jQuery ){
		return;
	}
	
	$("#networks_section_loading").css({
		left: $("#networks_section").position().left,
		top: $("#networks_section").position().top,
		width: $("#networks_section").width(),
		height: $("#networks_section").height() + ( $.browser.msie ? -8 : 0 ) // -8
																				// is a
																				// hack
																				// to
																				// fix
																				// ie
																				// since
																				// css
																				// hacks
																				// don't
																				// work
																				// :[
	});
}

function startNetworksLoader(){
	_networks_reloading_count++;
	// console.log("++ " + _networks_reloading_count);
		
	// $("#networks_section_loading").show();
	adjustNetworksLoaderCSS();
		
	return _networks_reloading_count;
}

function areNetworksReloading(){
	return _networks_reloading_count != 0;
}

function endNetworksLoader(){
	setTimeout(function(){
		_networks_reloading_count--;
		
		if( _networks_reloading_count < 0 ){
			_networks_reloading_count = 0;
		}
		
		// console.log("-- " + _networks_reloading_count);
		
		if(_networks_reloading_count <= 0){
			$("#networks_section_loading").hide();
		}
	}, 500);
}

function refreshCounts(grpId) {
	// console.log("refresh indiv");

	var allNetworks = $(".query_network.valid").find("input[type=checkbox][organism=" + $("#species_select").val() + "][group=" + grpId + "]");
	var allNetworksCount = allNetworks.size();
	var selectedNetworksCount = allNetworks.filter(":checked").size();
	$("#countAllNetworksFor" + grpId).text(allNetworksCount);
	$("#countSelectedNetworksFor" + grpId).text(selectedNetworksCount);
}

function updateParentChecks(){
	
	$(".query_group_checkbox").each(function(){
		var organism = $(this).attr("organism");
		var group = $(this).attr("group");
		var inputs = $(".query_network_checkbox[organism="+ organism +"][group="+ group +"]");
		
		var checked = inputs.filter(":checked").size() > 0;
		var half_checked = inputs.size() != inputs.filter(":checked").size();
		
		if( checked ){
			$(this).attr("checked", true);
		} else {
			$(this).attr("checked", null);
		}
		
		if( half_checked ){
			$(this).addClass("half_checked");
		} else {
			$(this).removeClass("half_checked");
		}
	});
	
}

function refreshAllGroupCounts() {
	// console.log("refresh all");

	var inputs = $(".query_network_checkbox[organism=" + $("#species_select").val() + "]");
	var totalNetworksCount = inputs.size();
	var totalSelectedNetworksCount = inputs.filter(":checked").size();
	
	$(".query_network_group").each(function() {
		var grpId = $(this).attr("group");
		if(grpId != 0){
			refreshCounts(grpId);
		}
	});
	$("#totalNetworksCount").text(totalNetworksCount);
	$("#totalSelectedNetworksCount").text(totalSelectedNetworksCount);
	
	if( totalNetworksCount == totalSelectedNetworksCount ) {
	    $("#network_selection_select_all").addClass("selected_sorting");
	    $("#network_selection_select_none").add("#network_selection_select_default").removeClass("selected_sorting");
	} else if( totalSelectedNetworksCount == 0 ) {
	    $("#network_selection_select_none").addClass("selected_sorting");
	    $("#network_selection_select_default").add("#network_selection_select_all").removeClass("selected_sorting");
	} else if( default_networks_selected() ) {
	    $("#network_selection_select_default").addClass("selected_sorting");
	    $("#network_selection_select_none").add("#network_selection_select_all").removeClass("selected_sorting");
	} else {
	    $("#network_selection_select_none").add("#network_selection_select_all").add("#network_selection_select_default").removeClass("selected_sorting");
	}
	
	$("#species_select option").each(function(){
		var org = $(this).attr("value");
		var section = $(".query_networks[group=0][organism="+org+"]");
		var selectedUserNetworksCount = section.find("input:checked").size();
		var userNetworksCount = section.find(".query_network.valid").size();

		$(".network-group-count .selected_count[group=0][organism="+org+"]").html(selectedUserNetworksCount);
		$(".network-group-count .all_count[group=0][organism="+org+"]").html(userNetworksCount);
	});
	
	
	
	// console.log("end hide");
}

$(function(){
	
	$(".trashIcn").live("mousedown", function(){
		var container = $(this).parents(".query_network");
    	var id = parseInt( container.attr("network") );
    	
    	deleteUserNetwork( container );
    	return false;
    });
	
});

function sortQueryNetworks() {
	// console.log("\nsorting query networks");

// if(lastSortingCriteria == queryNetworksSortingCriteria) {
// queryNetworksDescendingSorting = !queryNetworksDescendingSorting;
// }
	$("#networkTree").queryNetworksSort({criteria: queryNetworksSortingCriteria, descending: queryNetworksDescendingSorting});
	$("#network_sorting_sortByNetworkSize").add("#network_sorting_sortByFirstAuthor").add("#network_sorting_sortByLastAuthor").add("#network_sorting_sortByPubDate").removeClass("selected_sorting");
	$("#network_sorting_sortBy" + queryNetworksSortingCriteria).addClass("selected_sorting");
// lastSortingCriteria = queryNetworksSortingCriteria;
}

function restoreDefaultNetworks() {
	// console.log("--\n setting default networks");

	// reset all networks
	$("#networkTree input[organism=" + $("#species_select").val() + "]").filter("[default=false]").removeAttr("checked");
	// update networks
	$("#networkTree input[organism=" + $("#species_select").val() + "]").filter("[default=true]").attr("checked", "checked");
	// update groups
    $(".query_network_group").each(function() {
    	$(this).updateChecktreeItem();
    });
}

function default_networks_selected() {
	
	var inputs = $(".query_network_checkbox[organism=" + $("#species_select").val() + "]");
	var defaults = inputs.filter("[default=true]");
	var not_defaults = inputs.not("[default=true]");
	var unchecked_defaults = defaults.not(":checked");
	var checked_not_defaults = not_defaults.filter(":checked");
	
	return unchecked_defaults.size() == 0 && checked_not_defaults.size() == 0;
}

function refreshNetworks() {
	// console.log("refresh networks...");
	
	// restore previously selected networks
    var checkedList = window["checkedQueryNetworks"];

    if (checkedList != null) {
    	$(".query_networks input[type=checkbox]").attr("checked", false);
    	$.each(checkedList, function(i, id) {
            $("#"+id).attr("checked", true);
        });
        window["checkedQueryNetworks"] = null;
    }
	
    // Complete the networks tree styles:
    $(".query_network_group").each(function() {
    	$(this).updateChecktreeItem();
    	var grpId = $(this).attr("id").substring("queryGroup".length);
    	refreshCounts(grpId);
    });
    
    // validate again:
    validateNetworks();
}

function addNetworkCheckBoxListeners(){
	$(".query_network_group").live("click", function(evt) {
		if( $(evt.target).hasClass("query_group_checkbox") ){
			return;
		}
		
		var orgId = $(this).attr("organism");
		var groupId = $(this).attr("group");
		
		$(".query_networks").hide();
		$(".query_networks[organism=" + orgId + "][group=" + groupId + "]").show();
	
		// Highlight this item...
		$(".query_network_group, .query_network").removeClass("selected");
		$(this).removeClass("select_hover");
		$(this).addClass("selected");
		// ...and the info:
		$(".query_network_info[organism=" + $("#species_select").val() + "]").hide();
		
	    // adjust long network names
		$(".query_network label").each(function() {
			if($(this).width() > 270) {
				$(this).attr("tooltip", $(this).text());
			}
		});
	});
	
	$(".query_network_group input").live("click", function() {
		var checked = $(this).attr("checked") || $(this).hasClass("half_checked");
		
		var orgId = $(this).attr("organism");
		var groupId = $(this).attr("group");
		
		$(".query_networks[organism=" + orgId + "][group=" + groupId + "] input:checkbox").attr("checked", checked);
		
		$(this).updateChecktreeItem();
		validateTree();
		refreshCounts(groupId);
	});
	
	$(".query_network input").live("click", function(evt) {
		var groupId = $(this).attr("group");
		var organism = $(this).attr("organism");
		$(this).updateChecktreeItem();
		validateTree();
		refreshCounts(groupId);
		evt.stopPropagation();
	});
	
	$(".query_network").live("click", function(evt) {
		if( $(evt.target).hasClass("query_network_checkbox") || $(evt.target).parents().andSelf().hasClass("query_network_info") ){
			return;
		}
		
		var info = $(this).find(".query_network_info");
		
		$(".query_network").removeClass("selected");
		var visible = info.is(":visible");
		$(".query_network_info").filter("[organism=" + $("#species_select").val() + "]").hide();
		if (!visible) {
			$(this).addClass("selected");
			info.show();
			$(this).parent(".query_networks").scrollTo($(this), 50);
		}
	});
}

function updateInputNetworks() {
    refreshAllGroupCounts();
}

var queryNetworksSortingCriteria = "FirstAuthor";
var queryNetworksDescendingSorting = false;

