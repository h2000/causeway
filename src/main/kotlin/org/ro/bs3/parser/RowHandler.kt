package org.ro.bs3.parser

import org.ro.bs3.parser.BaseXmlHandler
import org.ro.to.bs3.Bs3Object
import org.ro.to.bs3.Bs3RowContent
import org.ro.to.bs3.Row

class RowHandler : BaseXmlHandler() {
    override fun doHandle() {
//        logEntry.aggregator = NavigationAggregator()
        //      update()
    }

    override fun parse(xmlStr: String): Bs3Object? {
        //TODO dive into sub elements, create objects and use in constructor

        val colOrClearFixVisibleOrClearFixHidden: List<Bs3RowContent>? = ArrayList<Bs3RowContent>()
        var metadataError = ""
        var id = ""
        val cssClass = ""
        return Row(colOrClearFixVisibleOrClearFixHidden, metadataError, id, cssClass)
    }
}
