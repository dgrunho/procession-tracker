package com.softinsa.sf2019_tracker

class TokenManager {
    var tokens:ArrayList<Token>? = null
    init {
        tokens = ArrayList()
        tokens!!.add(Token("QI6343", 1))
        tokens!!.add(Token("PE6751", 3))
        tokens!!.add(Token("IC1940", 501))
        tokens!!.add(Token("AO1721", 502))
        tokens!!.add(Token("UT5337", 503))
        tokens!!.add(Token("JL5476", 12))
        tokens!!.add(Token("RZ5021", 10))
    }

    fun getProcession(PIN:String?): Int {
        tokens!!.forEach {
            if (it.PIN.toUpperCase() == PIN!!.toUpperCase()){
                return it.ProcessionID
            }
        }
        return 0
    }
}