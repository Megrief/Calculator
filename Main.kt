package calculator
import java.math.BigInteger
import kotlin.math.pow
import kotlin.system.exitProcess

fun main() {
    Calculator()
}

class Calculator {
    private val varMap = mutableMapOf<String, String>()

    init {
        while (true) {
            firstCheck()
        }
    }

    private fun firstCheck(input: String = readln().trim()) {
        when {
            input.isEmpty() -> return
            input.first() == '/' -> {
                when (input) {
                    "/help" -> println("The program prints result of the calculations.")
                    "/exit" -> println("Bye!").also { exitProcess(0) }
                    else -> println("Unknown command")
                }
                firstCheck()
            }
            input.contains("=") -> equation(input)
            input.contains(Regex("\\([\\w\\s*/^+-]+\\)")) -> formPar(input)
            else -> calculation(preparing(input)).let { if (it == null) firstCheck() else println(it) }
        }
    }

    private fun invAssignCheck(input: String): Boolean {
        return !input.last().toString().matches(Regex("[\\w)]"))
                || input.contains(Regex("[^\\w\\s+*/()^-]|[a-zA-Z]\\d|\\d[a-zA-Z]"))
                || input.isEmpty()
    }
    private fun invIdentCheck(input: String): Boolean {
        return !input.matches(Regex("[a-zA-Z]+"))
    }
    private fun equation(input: String) {
        input.split(Regex("=+"), 2).let {
            val (left, right) = Pair(it.first().trim(), it.last().trim())
            when {
                invIdentCheck(left) -> println("Invalid assignment")
                invAssignCheck(right) -> println("Invalid identifier")
            }
            if (right.matches(Regex("-?\\d+"))) varMap[left] = right else {
                calculation(preparing(right)).let { num ->
                    if (num != null) {
                        varMap[left] = num.toString()
                    }
                }
            }
        }
    }
    private fun formPar(input: String) {
        val reg = Regex("\\([\\w\\s*/^+-]+\\)")
        var res = input
        while (res.contains(reg)) {
            reg.find(res)!!.value.let {
                val prepVal = it.filter { ch -> !ch.toString().matches(Regex("[()]")) }
                calculation(preparing(prepVal)).let { str ->
                    if (str != null) res = res.replace(it, str) else return firstCheck()
                }
            }
        }
        firstCheck(res)
    }
    private fun calcPow(input: String): String {
        val reg = Regex("\\d+\\^\\d+")
        var res = input
        while (res.contains(reg)) {
            reg.find(res)!!.value.let {
                val prepVal = it.split(Regex("\\^"))
                val calc = prepVal.first().toDouble().pow(prepVal.last().toInt()).toInt()
                res = res.replace(it, calc.toString())
            }
        }
        return res
    }
    private fun calcMultDiv(input: String): String {
        val reg = Regex("\\d+[*/]-?\\d+")
        var res = input
        while (res.contains(reg)) {
            reg.find(res)!!.value.let {
                val (left, right) = it.split(Regex("[*/]")).let { pair -> pair.first() to pair.last() }
                fun calc(big: Boolean, left: String, right: String, oper: Boolean = it.contains('/')): String {
                    return when {
                        big && oper -> (BigInteger(left) / BigInteger(right)).toString()
                        big && !oper -> (BigInteger(left) * BigInteger(right)).toString()
                        !big && oper -> (left.toInt() / right.toInt()).toString()
                        else -> (left.toInt() * right.toInt()).toString()
                    }
                }
                res = res.replace(it, calc((tooLong(left) || tooLong(right)), left, right))
            }
        }
        return res
    }
    private fun calcAdd(input: String): String {
        val reg = Regex("-?\\d+[+-]\\d+")
        var res = input
        while (res.contains(reg)) {
            reg.find(res)!!.value.let {
                val (left, right) = Regex("-?\\d+").findAll(it).let {
                        pair -> pair.first().value to pair.last().value
                }
                val sum = if (tooLong(left) || tooLong(right)) {
                    (BigInteger(left) + BigInteger(right)).toString()
                } else (left.toInt() + right.toInt()).toString()
                res = res.replace(it, sum)
            }
        }
        return res
    }
    private fun tooLong(num: String): Boolean {
        return num.toBigInteger() !in -BigInteger.TWO.pow(32)..BigInteger.TWO.pow(32) - BigInteger.ONE
    }
    private fun calculation(input: String): String? {
        return when {
            input.contains("Unknown variable") -> null.also { println("Unknown variable") }
            input.contains("Invalid expression") -> null.also { println("Invalid expression") }
            else -> {
                var res = input
                for (round in 1..4) {
                    res = when {
                        round == 1 && res.matches(Regex("-?\\d+")) -> res
                        round == 2 && res.contains("^") -> calcPow(res)
                        round == 3 && res.contains(Regex("[*/]")) -> calcMultDiv(res)
                        round == 4 && res.contains(Regex("[+-]")) -> calcAdd(res)
                        else  -> continue
                    }
                }
                if (res.trim().matches(Regex("-?\\d+"))) res else {
                    println("Invalid expression")
                    null
                }
            }
        }
    }
    private fun preparing(input: String): String {
        var res = input
        for (round in 1..6) {
            res = when {
                round == 1 -> if (invAssignCheck(res)) "Invalid expression" else res.replace(Regex("\\s+"), "")
                round == 2 && res.contains(Regex("[a-zA-Z]+")) -> formLetters(res)
                round == 3 && res.contains(Regex("\\*+|/+|\\^+")) -> formMDP(res)
                round == 4 && res.contains(Regex("\\+{2,}|-\\+|\\+-")) -> formPlus(res)
                round == 5 && res.contains(Regex("-+")) -> formMin(res)
                round == 6 && res.contains(Regex("\\d+")) -> res
                else -> continue
            }
        }
        return res
    }
    private fun formMDP(input: String): String {
        var res = input
        Regex("\\*+|/+|\\^+").findAll(input).forEach {
            res = if (it.value.length > 1) {
                "Invalid expression"
            } else res
        }
        return res
    }
    private fun formLetters(input: String): String {
        var res = input
        Regex("[a-zA-Z]+").findAll(input).forEach {
            res = if (it.value in varMap.keys) res.replace(it.value, varMap[it.value].toString()) else "Unknown variable"
        }
        return res
    }
    private fun formMin(input: String): String {
        var res = input
        Regex("-+").findAll(input).forEach {
            res = if (it.value.length % 2 == 0) res.replace(it.value, "+") else res.replace(it.value, "-")
        }
        return res
    }
    private fun formPlus(input: String): String {
        val reg = Regex("\\+{2,}|-\\+|\\+-")
        var res = input
        while (res.contains(reg)) {
            reg.find(res).let {
                if (it?.value != null) {
                    res = when {
                        it.value.matches(Regex("\\+{2,}")) -> res.replace(it.value, "+")
                        else -> res.replace(it.value, "-")
                    }
                }
            }
        }
        return res
    }
}
