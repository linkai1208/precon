(function(){

/**
 * Call precon.client.quickSearch to get a list of networks
 * 
 * No returns. This function will initialize/update the network table.
 * 
 */
Ext.define('Precon.controller.NetworkGridController', {
    extend: 'Precon.controller.BaseController',
    requires:['Precon.view.NetworkGrid', 'Precon.view.MyNetworkGrid'],
    init: function() {
     		console.log("NetworkGridController.init"); 
     		
     		this.control({
     			'#ingraph-search': {
  				  afterrender:this.autoCompleteSearch,
  				  scope:this
     			},
     			'networkgrid': {
     				  //afterrender:this.initApp,
     				  //itemclick: this.networkGridClicked,
     				 // itemdblclick:this.networkGridDblClicked,
     				  //select: this.networkGridSelect,
     				  //deselect: this.networkGridDeselect,
     				
     	        	itemdblclick:function(view, row){
     	        		console.log("double Clicked network! " + row.data._id)      
     	        		//showObject(row.data)     	        		
     	        	},
     	        	itemmouseenter:function(view, row){
     	        		var netId = row.data._id 
     	        		mygraph.highlight(netId, true)        		
     	        	},
     	        	itemmouseleave:function(view, row){
     	        		var netId = row.data._id
     	        		mygraph.highlight(netId, false)
     	        	},
     				scope: this
     			},
     			'mynetworkgrid':{
     				itemclick:function(view, row, el, rowIndex, evt){
     					console.log("clicked mynetwork", arguments)
     					if(evt && evt.target && evt.target.name == 'filterByNetwork'){
     						this.toggleMyNetwork(row.data, evt.target)
     					}     					
     				},
     				itemdblclick:function(view, row){
     	        		console.log("double Clicked my network! ", row.data)      
     	        		//showObject(row.data)     	        		
     	        	}
     			}
     		});     	
    },
    toggleMyNetwork:function(networkData, checkbox){
    	var networkStore= app.getStore('Networks')
    	if(networkStore.findExact("_id", networkData._id) >= 0)    	
    		return filterNetwork(checkbox); // already in the Network Grid list, delegate to it 
    	
    	var self=this
    	// assume haven't added to graph yet	
    	precon.getNetwork(networkData._id, function(network){
    		self.loadNetworks([network], true, false)
    	})
    },
    onLaunch:function(){
    	objid = this.getObjectIdFromUrl()	    	
    	if (objid){
    		
    		var self = this
    		precon.searchNetworks(objid, function (networkObjects) {
    			   console.log('here is the returns from TJ. ');
    			   console.log(networkObjects);
    				if(!networkObjects || networkObjects.length == 0){
    					console.log("Error: no result")
    					return
    				}    			
    				if(networkObjects.length == 1 && objid== networkObjects[0].get('id')){
    					self.getGraphModel().setGraphNetwork(networkObjects[0])
    				}
    				self.loadNetworks(networkObjects, true);
    				self.loadMyNetworks();
    			});			
    	}
    	$(document).on(precon.event.UserLogin, this.loadMyNetworks)    	
    },
   
    loadMyNetworks: function(){
    	console.log("loadMyNetworks")
    	if(!app.getUser()) return;  // not logged in
    	var networkStore= app.getStore('MyNetworks')
    	precon.getNetworksByUser(app.getUser().user_id, function(networkObjects){
    		networkObjects.forEach(function(network){
    			if(networkStore.findExact("_id", network.get('_id')) <0  ){ // add only if not already exists				
    				obj = network.getRawdata()
    				obj.include = false		
    				networkStore.add( obj )
    				//console.log('load obj into network table ');
    				//console.log(obj);
    			}		
    		});
    	})
    	
    },
   
    /**
	 * 
	 * @param networkObjects
	 * @param toGraph: whether to draw on graph immediately
	 * @param toReplace: remove existing before adding new one
	 */
   loadNetworks: function(networkObjects, toGraph, toReplace){
		if(!networkObjects) return
		if(toReplace){
			this.getGraphModel().removeAll();
			networkStore.removeAll();
		}		
		var networkStore= app.getStore('Networks')
		console.log(networkObjects);
		console.log('is the networkobjects');
		var self = this;
		networkObjects.forEach(function(network){
			if(toGraph) self.getGraphModel().addNetwork( network);
			if(networkStore.findExact("_id", network.get('_id')) <0  ){ // add only if not already exists				
				obj = network.getRawdata()
				obj.include = toGraph		
				networkStore.add( obj )
				//console.log('load obj into network table ');
				//console.log(obj);
			}		
		})	
	},
	autoCompleteSearch: function() {
   	    var self = this;
	    $( "#ingraph-search-inputEl" ).autocomplete({
	          source: validateKeyword,
	          minLength:2,
	          select: function(event, ui) {
	              console.log("selected ", ui)
	              precon.searchNetworks(ui.item._id, function(networks){ self.loadNetworks(networks, false)})
	          }         
	        });
   }
	
}); // end Controller

})();

function filterNetwork(item, groupingFeature){	
	var graphModel = app.graphModel
	if(item.name == 'filterByNetwork' ){
		if(item.checked)
			graphModel.addNetwork( item.value )
		else
			graphModel.removeNetwork( item.value )
		
		// check the group checkbox accordingly
		var grp = item.getAttribute("belongtogroup") || ""
		if(item.checked || $("input[belongtogroup=" + grp+"]:checked").length>0)
			$("input[group=" + grp+"]")[0].checked = true
		else
			$("input[group=" + grp+"]")[0].checked = false		
			
		console.log("filter!", item.value)
		// same network checkbox may appear multiple times, i.e., on mynetworksgrid
		$("input[value="+ item.value+"]").attr("checked", item.checked)
	}
	if(item.name == 'filterByGroup'){
		var grp = item.getAttribute("group") || ""
		
		if(groupingFeature){
			// keep the group expanded if clicked on the group checkbox
			var rows = groupingFeature.view.getEl().query('.x-grid-group-body');
	        Ext.each(rows, function(row) {        	
	        	if( $(row).find("input[belongtogroup="+ grp+"]").length>0)
	        		groupingFeature.expand(Ext.get(row));
	        });			
		}
		
		// find all the networks in this group
		$("input[belongtogroup="+ grp+"]").each(function(indx, networkItemCheckbox){
			if( (  item.checked && !networkItemCheckbox.checked) || networkItemCheckbox.checked) {
				// select network if not already selected
				$(networkItemCheckbox).click()
			}		
		});		
	}
	if(item.name == 'filterAll'){
		$("input[group]").each(function(indx, groupCheckbox){
			if( (  item.checked && !groupCheckbox.checked) || groupCheckbox.checked) 
				$(groupCheckbox).click()
		})		
	}
	
	// toggle select all checkbox
	if( $("input[name=filterByNetwork]:checked").length>0 ){
		$("input[name=filterAll]")[0].checked = true
	}
	else{
		$("input[name=filterAll]")[0].checked = false
	}	
}	