package com.example.melon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DataMelonFragment : Fragment() {
    private lateinit var listMelon: RecyclerView
    private val list = ArrayList<Melon>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_data_melon, container, false)
        listMelon = rootView.findViewById(R.id.list_melon)
        listMelon.setHasFixedSize(true)

        list.addAll(getListMelon())
        showRecycleList()
        // Inflate the layout for this fragment
        return rootView
    }

    private fun getListMelon(): ArrayList<Melon> {
        val melonName = resources.getStringArray(R.array.melon_name)
        val melonResult = resources.getStringArray(R.array.melon_result)
        val melonDate = resources.getStringArray(R.array.melon_date)
        val melonPhoto = resources.obtainTypedArray(R.array.melon_photo)
        val dataMelon = ArrayList<Melon>()
        for (i in melonName.indices){
            val melon = Melon(melonName[i], melonResult[i], melonDate[i], melonPhoto.getResourceId(i, -1))
            dataMelon.add(melon)
        }
        return dataMelon
    }

    private fun showRecycleList() {
        listMelon.layoutManager = LinearLayoutManager(requireContext())
        val listMelonAdapter = ListMelonAdapter(list)
        listMelon.adapter = listMelonAdapter
    }

}