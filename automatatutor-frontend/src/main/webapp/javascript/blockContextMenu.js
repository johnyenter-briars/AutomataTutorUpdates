/*******************************
 * Actual Contextmenu Call
 */
let contextOpen;
d3.contextMenu = function (menu, openCallback) {

    // create the div element that will hold the context menu
    // top level context menu
    d3.select('body').selectAll('.d3-context-menu').data([1])
        .enter()
        .append('div')
        .attr('class', 'd3-context-menu');

    // close menu
    d3.select('body').on('click.d3-context-menu', function() {
        d3.select('.d3-context-menu').style('display', 'none');
        contextOpen = false;
    });

    // this gets executed when a contextmenu event occurs
    return function(data, index) {
        // Avoid multiple contextmenus being triggered at the same time
        d3.event.stopPropagation();
        // Captures the this pointer in case we need it later on	
        var elm = this;
        contextOpen = true;
        d3.selectAll('.d3-context-menu').html('');
        var list = d3.selectAll('.d3-context-menu').append('ul');
        list.selectAll('li').data(menu).enter()
            .append('li')
            .html(function(d) {
                return d.title;
            })
            .on('click', function(d, i) {
                d.action(elm, data, index);
                // Try to remove it?
                d3.select('.d3-context-menu').style('display', 'none');
                contextOpen = false;
            });

        // the openCallback allows an action to fire before the menu is displayed
        // an example usage would be closing a tooltip
        if (openCallback) openCallback(data, index);

        // display context menu
        d3.select('.d3-context-menu')
            .style('left', (d3.event.pageX - 2) + 'px')
            .style('top', (d3.event.pageY - 2) + 'px')
            .style('display', 'block');

        d3.event.preventDefault();
    };
};
