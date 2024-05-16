package com.example.melon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ListMelonAdapter (private val listMelon: ArrayList<Melon>): RecyclerView.Adapter<ListMelonAdapter.ListViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.list_item_melon, parent, false)
        return ListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val (name, result, date, photo) = listMelon[position]
        holder.imgPhoto.setImageResource(photo)
        holder.melonName.text = name
        holder.melonResult.text = result
        holder.melonDate.text = date
    }

    override fun getItemCount(): Int = listMelon.size

    class ListViewHolder (itemView: View): RecyclerView.ViewHolder(itemView){
        val imgPhoto: ImageView = itemView.findViewById(R.id.img_item_photo)
        val melonName: TextView = itemView.findViewById(R.id.item_name)
        val melonResult: TextView = itemView.findViewById(R.id.item_result)
        val melonDate: TextView = itemView.findViewById(R.id.item_date)
    }

}