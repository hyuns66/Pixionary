package com.example.findmyphoto

import android.util.Log
import java.lang.Integer.max
import java.lang.Math.min
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets


class SimpleTokenizer {

    var encoder : Map<String, Int>
    var bpeRanks : Map<Pair<String, String>, Int>
    val byteEncoder: Map<Int, Char> = bytesToUnicode()  // (유니코드 정수, 문자)
    val byteDecoder: Map<Char, Int> = byteEncoder.entries.associate { (k, v) -> v to k }    // (문자, 유니코드 정수)

    init{
        val merges = loadMerges()
        val vocab = loadVocab(merges)
        encoder = vocab.zip(0 until vocab.size).toMap()
        bpeRanks = merges.zip(0 until merges.size).toMap()
    }

    fun ord(c: Char): Int = c.code
    fun bytesToUnicode(): Map<Int, Char> {
        val bs: MutableList<Int> = mutableListOf()

        // bs 리스트에 유니코드 특수문자 추가
        bs.addAll((ord('!')..ord('~')).toList())
        bs.addAll((ord('¡')..ord('¬')).toList())
        bs.addAll((ord('®')..ord('ÿ')).toList())

        val cs = bs.toMutableList()
        var n = 0
        for (b in 0 until 256) {
            if (b !in bs) {
                bs.add(b)
                cs.add(256 + n)
                n += 1
            }
        }

        val mapping = bs.zip(cs).associate { Pair(it.first, it.second.toChar()) }
        return mapping
    }
    fun whitespaceClean(text: String): String {
        var cleanedText = text.replace("\\s+".toRegex(), " ") // 큰 공백을 한칸 공백으로 치환    "  King     kin   g    " -> " King kin g "
        cleanedText = cleanedText.trim()
        return cleanedText
    }

    fun loadMerges(): List<Pair<String, String>> {
        // 안드로이드에서 파일 접근 시 Context를 사용하여 파일 경로를 얻습니다.
        // 예제에서는 경로를 직접 지정합니다. 실제 사용 시에는 적절한 파일 경로를 설정해야 합니다.
        val fileInputStream = ApplicationClass.getContext().resources.openRawResource(bpeVocabFilePath)

        fileInputStream.use { inputStream ->
            val merges = inputStream.reader(Charset.defaultCharset()).readText().split('\n')
            return merges.drop(1).take(49152 - 256 - 2).map {
                val parts = it.split(" ")
                parts[0] to parts[1]
            }
        }
    }
    fun loadVocab(merges : List<Pair<String, String>>) : List<String>{
        var vocab : List<String> = bytesToUnicode().values.toList().map{ it.toString() }
        vocab = vocab + vocab.map { "$it</w>" }
        vocab = vocab + merges.map {
            it.first + it.second
        }
        vocab = vocab + listOf("<|startoftext|>", "<|endoftext|>")
        return vocab
    }

    fun getPairs(word : MutableList<String>) : Set<Pair<String, String>>{
        val pairs = mutableSetOf<Pair<String, String>>()
        var prev_char = word[0]
        for (w in word.subList(1, word.size)){
            val w_list = Pair(prev_char, w)
            pairs.add(w_list)
            prev_char = w
        }
        return pairs
    }
    fun bpe(token : String) : String{
        // bpe가 필요없는 토큰인 경우 바로 리턴
        if (cache.containsKey(token)) {
            return cache[token]!!
        }
        // 'king' -> ('k', 'i', 'n', 'g</w>')
        var word = mutableListOf<String>()
        for ((index, text) in token.withIndex()){
            var w = text.toString()
            if (index == token.length-1){
                w += "</w>"
            }
            word.add(w)
        }
        // ('k', 'i', 'n', 'g</w>') -> {('k', 'i'), ('i', 'n'), ('n', 'g</w>')}
        var pairs = getPairs(word)
        if (pairs.isEmpty()) {
            return token + "</w>"
        }
        while(true){
            val bigram = pairs.minByOrNull{ pair -> bpeRanks[pair] ?: Int.MAX_VALUE}
            if (bigram !in bpeRanks || bigram == null) {
                break
            }
            val first = bigram.first
            val second = bigram.second
            val new_word = mutableListOf<String>()
            var i = 0
            Log.d("word", word.toString())
            while(i < word.size){
                try{
                    for(j in i..word.size){   // catch문에서 적절한 처리하기위해 마지막까지 first 못찾는 경우 일부러 outOfBoundsException 일으킴
                        if (word[j] == first){      // i 이후로 first에 해당하는 글자 찾으면 인덱스 j에 반환
                            new_word += word.slice(i..< j)  // first 이전의 글자들은 그대로 복사
                            i = j       // first 이후부터 재탐색
                            break
                        }
                    }
                } catch (e : IndexOutOfBoundsException){    // first에 해당하는 글자 못찾으면 전부 복사하고 break
                    new_word += word.slice(i..word.lastIndex)
                    break
                }
                if (word[i] == first && i < word.lastIndex && word[i+1] == second){ // first 뒤에 second가 붙어있으면 하나로 merge
                    new_word.add(first+second)
                    i += 2
                } else {
                    new_word.add(word[i])
                    i += 1
                }
            }
            Log.d("word_new", new_word.toString())
            word = new_word
            // word가 하나의 단어가 되면 break 아니면 pairs 갱신 후 반복
            if (word.size == 1){
                break
            } else {
                pairs = getPairs(word)
            }
        }
        val result_word = ' ' + word[0]
        cache[token] = result_word
        Log.d("word_result", result_word)
        return result_word
    }

    fun encode(text : String) : List<Int>{
        val bpeTokens = mutableListOf<Int>()

        pattern.findAll(text).forEach {
            val tokenBytes = it.value.toByteArray(StandardCharsets.UTF_8)
            val token = tokenBytes.map { byte -> byteEncoder[byte.toInt()] ?: "" }.joinToString("")
            val bpeTokenized = bpe(token).split(' ')
            bpeTokens.addAll(bpeTokenized.mapNotNull { encoder[it] })
            Log.d("word_bpe", bpeTokens.toString())
        }
        return bpeTokens
    }

    fun tokenize(text: String, n_text: Int = 76): Pair<IntArray, Int> {
        val sot = this.encoder["<|startoftext|>"]!!
        val eot = this.encoder["<|endoftext|>"]!!
        var tokens = encode(whitespaceClean(text.trim()))
        // 문장 시작, 끝  토큰 추가
        tokens = listOf(sot) + tokens.slice(
            0..min(n_text - 1, tokens.lastIndex)
        ) + listOf(eot)
        return Pair((tokens + List(n_text + 1 - tokens.size) { 0 }).toIntArray(), tokens.size)    // 남은 공간 0패딩
    }

    companion object{
        val pattern = Regex("""<\|startoftext\|>|<\|endoftext\|>|'s|'t|'re|'ve|'m|'ll|'d|\p{L}+|\p{N}|[^\s\p{L}\p{N}]+""", RegexOption.IGNORE_CASE)
        val bpeVocabFilePath = R.raw.bpe_simple_vocab_16e6
        val cache = mutableMapOf("<|startoftext|>" to "<|startoftext|>", "<|endoftext|>" to "<|endoftext|>")
    }
}