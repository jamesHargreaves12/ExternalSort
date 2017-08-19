package uk.ac.cam.jh2045.fjava.tick0;

import java.util.Arrays;

public class MinimumHeap {
	private int[] heap;
	private int[] values;
	
	private int size;

	public boolean isEmpty()
	{
		return size == 0;
	}
	
	public MinimumHeap(int[] initialValues){
	    this.heap = new int[initialValues.length];
	    for(int i = 0; i < initialValues.length; i++)
	    {
	    		heap[i] = i;
	    }
	    values = initialValues;
	    this.size = initialValues.length;
	    for(int i = size/2-1; i >= 0; i --)
	    {
	    		heapify(i);
	    }
	}

	private int getLeftChild(int position){
	    return 2*position+1;
	}

	private int getRightChild(int position){
	    return 2*position+2;
	}

	private void swap(int position1, int position2){
	    int temp = heap[position1];
	    heap[position1] = heap[position2];
	    heap[position2] = temp;
	}

	private boolean isLeaf(int position){
	    return position >= size/2;
	}

	public int minBlock()
	{
		//return which block contains minimum value
		return heap[0];
	}
	
	public void replace(int newValue){
		// replace the current minimum block value with a new Value and then reHeapify
		int block = heap[0];
		values[block] = newValue;
		if(!isLeaf(0)) heapify(0);
	}
	
	public void deleteHead()
	{
		heap[0] = heap[--size];
		heapify(0);
	}
	
//	private void heapify(int position){
//	    int posVal = values[heap[position]];
//	    int posRightChild = getRightChild(position);
//	    int posLeftChild = getLeftChild(position);
//	    	int valLeftChild = values[heap[posLeftChild]];
//	    int valRightChild = posRightChild >= size ? Integer.MAX_VALUE : values[heap[posRightChild]];
//
//	    if (posVal > valLeftChild || posVal > valRightChild){
//	        if(posRightChild >= size || valLeftChild < valRightChild)
//	        {
//	            swap(position, posLeftChild);
//	            if(!isLeaf(posLeftChild)) heapify(posLeftChild);
//	        }
//	        else
//	        {
//	            swap(position, posRightChild);
//	            if(!isLeaf(posRightChild)) heapify(posRightChild);
//	        }
//	    }
//	}
	
	private void heapify(int position) {
		int index = heap[position];
		int val = values[index];
		
	    while(!isLeaf(position)) {
	    		int minChildLoc = this.getMinChild(position);
	    		if(val <= values[heap[minChildLoc]]) {
    				break;
	    		}
    			heap[position] = heap[minChildLoc];
    			position = minChildLoc;
	    } 
		heap[position] = index;
	}	
	
	private int getMinChild(int position) {
		int right = getRightChild(position);
		int left = getLeftChild(position);
		return (right >= size || values[heap[left]] < values[heap[right]]) ? left : right;
	}	
}
